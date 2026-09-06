package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.CompetitionRegistrationWindowChangeRequest;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionRegistrationWindowChange;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.RegistrationWindowChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRegistrationWindowChangeRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionRegistrationWindowServiceTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionRegistrationWindowChangeRepository changeRepository;
    @Mock private BracketRepository bracketRepository;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private AusenciaTomadaSeguidorLinhaRepository ausenciaFollowRepository;
    @Mock private RoundSumoRepository roundSumoRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private CompetitionRegistrationWindowService service;

    private Competition competition;
    private UserAccount organizacao;

    @BeforeEach
    void setup() {
        competition = new Competition();
        competition.setId(10L);
        competition.setNome("RRC Teste");
        competition.setInicioInscricoes(LocalDate.now().minusDays(10));
        competition.setFimInscricoes(LocalDate.now().plusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(10));
        competition.setDataFim(LocalDate.now().plusDays(11));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setAtivo(true);

        organizacao = new UserAccount();
        organizacao.setId(20L);
        organizacao.setNome("Organização");
        organizacao.setRole(UserRole.ORGANIZACAO);
        organizacao.setAtivo(true);
    }

    @Test
    void prorrogaInscricoesAbertasEPreservaStatus() {
        LocalDate antiga = competition.getFimInscricoes();
        LocalDate nova = antiga.plusDays(3);
        prepararBase();
        when(changeRepository.save(any(CompetitionRegistrationWindowChange.class))).thenAnswer(invocation -> {
            CompetitionRegistrationWindowChange entity = invocation.getArgument(0);
            entity.setId(30L);
            return entity;
        });

        var result = service.alterar(10L, request(nova, "Mais prazo para equipes"));

        assertEquals(RegistrationWindowChangeType.PRORROGACAO, result.getTipo());
        assertEquals(antiga, result.getDataFimAnterior());
        assertEquals(nova, competition.getFimInscricoes());
        assertEquals(StatusCompetition.INSCRICOES_ABERTAS, competition.getStatus());
    }

    @Test
    void reabreEncerradaEInvalidaChaveAtualSemAtividade() {
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setFimInscricoes(LocalDate.now().minusDays(1));
        Bracket bracket = new Bracket();
        bracket.setId(40L);
        bracket.setAtual(true);
        bracket.setAtivo(true);
        bracket.setStatus(StatusBracket.GERADO);

        prepararBase();
        when(bracketRepository.findByCompetitionIdAndAtualTrueAndAtivoTrueOrderByDataCadastroDesc(10L))
                .thenReturn(List.of(bracket));
        when(changeRepository.save(any(CompetitionRegistrationWindowChange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.alterar(10L, request(LocalDate.now().plusDays(2), "Reabertura excepcional"));

        assertEquals(RegistrationWindowChangeType.REABERTURA, result.getTipo());
        assertEquals(StatusCompetition.INSCRICOES_ABERTAS, competition.getStatus());
        assertEquals(false, bracket.getAtual());
        assertEquals(StatusBracket.CANCELADO, bracket.getStatus());
        verify(bracketRepository).saveAll(List.of(bracket));
    }

    @Test
    void bloqueiaReaberturaQuandoFollowJaTemAtividade() {
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setFimInscricoes(LocalDate.now().minusDays(1));
        prepararBase();
        when(tentativaRepository.existsByRegistrationCompetitionId(10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.alterar(10L, request(LocalDate.now().plusDays(2), "Tentar reabrir")));

        verify(competitionRepository, never()).save(any());
        verify(bracketRepository, never()).saveAll(any());
    }

    @Test
    void bloqueiaReaberturaQuandoFollowJaTemTomadaPerdidaPorAusencia() {
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setFimInscricoes(LocalDate.now().minusDays(1));
        prepararBase();
        when(ausenciaFollowRepository.existsByRegistrationCompetitionId(10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.alterar(10L, request(LocalDate.now().plusDays(2), "Tentar reabrir")));

        verify(competitionRepository, never()).save(any());
        verify(bracketRepository, never()).saveAll(any());
    }

    @Test
    void novaDataNaoPodePassarDoInicioDaCompeticao() {
        prepararBase();

        assertThrows(IllegalArgumentException.class,
                () -> service.alterar(10L, request(competition.getDataInicio().plusDays(1), "Prazo inválido")));

        verify(competitionRepository, never()).save(any());
    }

    private void prepararBase() {
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(competitionRepository.findById(10L)).thenReturn(Optional.of(competition));
    }

    private CompetitionRegistrationWindowChangeRequest request(LocalDate novaData, String motivo) {
        CompetitionRegistrationWindowChangeRequest request = new CompetitionRegistrationWindowChangeRequest();
        request.setNovaDataFim(novaData);
        request.setMotivo(motivo);
        return request;
    }
}
