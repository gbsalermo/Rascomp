package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RoundSumo;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundSumoServiceTest {

    @Mock private RoundSumoRepository roundRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ConfigSumoRepository configSumoRepository;
    @Mock private InspecaoSumoService inspecaoSumoService;
    @Mock private MatchResultService matchResultService;

    @InjectMocks private RoundSumoService service;

    private Match match;
    private Registration a;
    private Registration b;
    private ConfigSumo config;
    private List<RoundSumo> persisted;

    @BeforeEach
    void setup() {
        CompetitionCategory category = CompetitionCategory.builder()
                .id(5L).nome("Mini Sumô").modalidade(Modalidade.SUMO).ativo(true).build();
        Bracket bracket = new Bracket();
        bracket.setId(30L);
        bracket.setCategory(category);
        bracket.setAtual(true);
        bracket.setAtivo(true);

        a = registration(101L, "Atlas Teste");
        b = registration(102L, "Boreal Teste");

        match = new Match();
        match.setId(200L);
        match.setBracket(bracket);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        match.setStatus(StatusMatch.AGENDADA);
        match.setAtivo(true);

        config = ConfigSumo.builder()
                .competitionCategory(category)
                .pesoMax(new BigDecimal("0.500"))
                .exigeInspecao(true)
                .maxTentativasInspecao(2)
                .numeroRounds(3)
                .roundsParaVencer(2)
                .permiteRoundDesempate(true)
                .build();

        persisted = new ArrayList<>();
        when(matchRepository.findById(200L)).thenReturn(Optional.of(match));
        when(configSumoRepository.findByCompetitionCategoryId(5L)).thenReturn(Optional.of(config));
        when(inspecaoSumoService.estaAptaParaCompetir(101L)).thenReturn(true);
        when(inspecaoSumoService.estaAptaParaCompetir(102L)).thenReturn(true);
        when(roundRepository.countByMatchId(200L)).thenAnswer(i -> (long) persisted.size());
        when(roundRepository.findByMatchIdOrderByNumeroRoundAsc(200L)).thenAnswer(i -> List.copyOf(persisted));
        when(roundRepository.save(any(RoundSumo.class))).thenAnswer(invocation -> {
            RoundSumo round = invocation.getArgument(0);
            round.setId((long) persisted.size() + 1);
            persisted.add(round);
            return round;
        });
        when(registrationRepository.findById(101L)).thenReturn(Optional.of(a));
        when(registrationRepository.findById(102L)).thenReturn(Optional.of(b));
    }

    @Test
    void deveRegistrarVitoriaNormalComUmaPenalidade() {
        RoundSumoDTO result = service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 1, 1));

        assertEquals(1, result.getNumeroRound());
        assertEquals(1, result.getPenalidadesA());
        assertEquals(1, result.getPenalidadesB());
        assertEquals(101L, result.getWinnerRegistrationId());
        assertEquals(StatusMatch.EM_ANDAMENTO, match.getStatus());
        verify(matchResultService, never()).criarAutomaticoSumo(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void duasPenalidadesDeADevemDarVitoriaAutomaticaParaB() {
        RoundSumoDTO result = service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 2, 0));

        assertEquals(102L, result.getWinnerRegistrationId());
        assertEquals(MotivoResultadoRoundSumo.PENALIDADES, result.getMotivoResultado());
        assertEquals(StatusRoundSumo.FINALIZADO, result.getStatus());
    }

    @Test
    void duasPenalidadesDeBDevemDarVitoriaAutomaticaParaA() {
        RoundSumoDTO result = service.registrar(round(102L, MotivoResultadoRoundSumo.DISPUTA, 0, 2));

        assertEquals(101L, result.getWinnerRegistrationId());
        assertEquals(MotivoResultadoRoundSumo.PENALIDADES, result.getMotivoResultado());
    }

    @Test
    void doisLadosNaoPodemTerminarRoundComDuasPenalidades() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 2, 2)));
    }

    @Test
    void suicidioWoDeveRegistrarVitoriaDoAdversario() {
        RoundSumoDTO result = service.registrar(round(102L, MotivoResultadoRoundSumo.SUICIDIO_WO, 0, 0));

        assertEquals(MotivoResultadoRoundSumo.SUICIDIO_WO, result.getMotivoResultado());
        assertEquals(102L, result.getWinnerRegistrationId());
    }

    @Test
    void penalidadeAcimaDeDoisDeveSerRejeitada() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 3, 0)));
    }

    @Test
    void segundaVitoriaDeveFecharBatalhaPeloServicoDeResultado() {
        service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 0, 0));
        service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 1, 0));

        verify(matchResultService).criarAutomaticoSumo(match, a, 2, 0);
    }

    @Test
    void chaveHistoricaNaoAceitaNovoRound() {
        match.getBracket().setAtual(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(round(101L, MotivoResultadoRoundSumo.DISPUTA, 0, 0)));
    }

    private RoundSumoDTO round(Long winner, MotivoResultadoRoundSumo motivo, int penaltyA, int penaltyB) {
        RoundSumoDTO dto = new RoundSumoDTO();
        dto.setMatchId(200L);
        dto.setWinnerRegistrationId(winner);
        dto.setStatus(StatusRoundSumo.FINALIZADO);
        dto.setMotivoResultado(motivo);
        dto.setPenalidadesA(penaltyA);
        dto.setPenalidadesB(penaltyB);
        return dto;
    }

    private Registration registration(Long id, String robotName) {
        Robot robot = new Robot();
        robot.setId(id + 1000);
        robot.setNome(robotName);
        robot.setAtivo(true);

        Registration registration = new Registration();
        registration.setId(id);
        registration.setRobot(robot);
        registration.setAtivo(true);
        return registration;
    }
}
