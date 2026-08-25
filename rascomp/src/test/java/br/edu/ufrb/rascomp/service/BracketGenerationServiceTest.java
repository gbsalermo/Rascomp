package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class BracketGenerationServiceTest {

    @Mock private BracketRepository bracketRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private BracketProgressionService bracketProgressionService;
    @Mock private InspecaoSumoService inspecaoSumoService;

    @InjectMocks
    private BracketGenerationService service;

    private Competition competition;
    private CompetitionCategory categorySumo;

    @BeforeEach
    void setUp() {
        competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC Teste");
        competition.setAtivo(true);

        categorySumo = CompetitionCategory.builder()
                .id(1L)
                .nome("Mini Sumô")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();
    }

    @Test
    void tresParticipantesAptosDevemGerarChaveDeQuatroComUmBye() {
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categorySumo));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(1L, 1L)).thenReturn(List.of());

        List<Registration> participantes = List.of(
                registration(1L), registration(2L), registration(3L));

        when(registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        eq(1L), eq(1L), eq(StatusRegistration.APROVADA)))
                .thenReturn(participantes);
        participantes.forEach(registration ->
                when(inspecaoSumoService.estaAptaParaCompetir(registration.getId())).thenReturn(true));

        when(bracketRepository.save(any(Bracket.class))).thenAnswer(invocation -> {
            Bracket bracket = invocation.getArgument(0);
            if (bracket.getId() == null) bracket.setId(50L);
            return bracket;
        });

        List<Match> partidasSalvas = new ArrayList<>();
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Match> partidas = invocation.getArgument(0);
            partidas.forEach(partidasSalvas::add);
            return partidasSalvas;
        });

        BracketDTO resultado = service.gerar(1L, 1L);

        assertEquals(StatusBracket.GERADO, resultado.getStatus());
        assertTrue(resultado.getAtual());
        assertEquals(3, partidasSalvas.size());
        assertEquals(2, partidasSalvas.stream().filter(m -> m.getRodada() == 1).count());
        assertEquals(1, partidasSalvas.stream().filter(m -> m.getStatus() == StatusMatch.BYE).count());
        assertEquals(1, partidasSalvas.stream()
                .filter(m -> m.getStatus() == StatusMatch.AGUARDANDO_PARTICIPANTES)
                .count());
        verify(bracketProgressionService, times(1)).avancarBye(any(Match.class));
    }

    @Test
    void inscricaoNaoAptaNaoDeveEntrarNaChave() {
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(categorySumo));

        Registration apta = registration(1L);
        Registration naoApta = registration(2L);

        when(registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        eq(1L), eq(1L), eq(StatusRegistration.APROVADA)))
                .thenReturn(List.of(apta, naoApta));
        when(inspecaoSumoService.estaAptaParaCompetir(1L)).thenReturn(true);
        when(inspecaoSumoService.estaAptaParaCompetir(2L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.gerar(1L, 1L));
    }

    @Test
    void deveRejeitarChaveamentoParaFollowLine() {
        CompetitionCategory follow = CompetitionCategory.builder()
                .id(3L)
                .nome("Seguidor de Linha")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(follow));

        assertThrows(IllegalArgumentException.class, () -> service.gerar(1L, 3L));
    }

    private Registration registration(Long id) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setCompetition(competition);
        registration.setCategory(categorySumo);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);
        return registration;
    }
}
