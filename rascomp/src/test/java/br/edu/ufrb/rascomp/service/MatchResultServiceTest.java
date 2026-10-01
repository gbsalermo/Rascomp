package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock private MatchResultRepository resultRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private BracketProgressionService bracketProgressionService;
    @Mock private RoundSumoRepository roundSumoRepository;
    @Mock private CompetitionContextService competitionContextService;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private MatchResultService service;

    @Test
    void devPodeCorrigirVencedorAntesDaDependenciaSeguinteComecar() {
        Competition competition = new Competition();
        competition.setId(1L);

        CompetitionCategory category = new CompetitionCategory();
        category.setModalidade(Modalidade.SUMO);

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCompetition(competition);
        bracket.setCategory(category);
        bracket.setAtivo(true);
        bracket.setAtual(true);

        Robot robotA = new Robot();
        robotA.setNome("Atlas");
        Registration a = new Registration();
        a.setId(101L);
        a.setRobot(robotA);

        Robot robotB = new Robot();
        robotB.setNome("Boreal");
        Registration b = new Registration();
        b.setId(102L);
        b.setRobot(robotB);

        Match match = new Match();
        match.setId(20L);
        match.setBracket(bracket);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        match.setStatus(StatusMatch.FINALIZADA);
        match.setAtivo(true);

        MatchResult persisted = new MatchResult();
        persisted.setId(30L);
        persisted.setMatch(match);
        persisted.setWinner(a);
        persisted.setPontosA(2);
        persisted.setPontosB(1);

        UserAccount dev = new UserAccount();
        dev.setId(40L);
        dev.setNome("DEV");
        dev.setRole(UserRole.DEV);
        dev.setAtivo(true);

        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));
        when(resultRepository.findByMatchId(20L)).thenReturn(Optional.of(persisted));
        when(registrationRepository.findById(102L)).thenReturn(Optional.of(b));
        when(resultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultDTO corrected = service.corrigirExcepcionalDev(
                20L,
                102L,
                "Vencedor registrado incorretamente pela mesa.");

        assertEquals(102L, corrected.getWinnerRegistrationId());
        assertEquals("Vencedor registrado incorretamente pela mesa.", corrected.getCorrectionReason());
        assertEquals(40L, corrected.getCorrectedByUserId());
        verify(bracketProgressionService).corrigirVencedor(match, a, b);
    }

    @Test
    void deveBloquearCriacaoManualDeResultadoParaSumo() {
        Match match = matchDaModalidade(Modalidade.SUMO);
        when(matchRepository.findById(20L)).thenReturn(Optional.of(match));

        MatchResultDTO dto = dto(20L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(resultRepository, registrationRepository, bracketProgressionService);
    }

    @Test
    void deveBloquearMatchResultParaFollowLine() {
        Match match = matchDaModalidade(Modalidade.FOLLOW_LINE);
        when(matchRepository.findById(21L)).thenReturn(Optional.of(match));

        MatchResultDTO dto = dto(21L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(resultRepository, registrationRepository, bracketProgressionService);
    }

    @Test
    void deveCriarResultadoAutomaticoSumoFinalizarMatchEAvancarVencedor() {
        CompetitionCategory category = new CompetitionCategory();
        category.setModalidade(Modalidade.SUMO);

        Bracket bracket = new Bracket();
        bracket.setCategory(category);
        bracket.setAtivo(true);
        bracket.setAtual(true);

        Robot robot = new Robot();
        robot.setNome("Postman Sumo A");

        Registration winner = new Registration();
        winner.setId(7L);
        winner.setRobot(robot);

        Match match = new Match();
        match.setId(20L);
        match.setBracket(bracket);
        match.setStatus(StatusMatch.EM_ANDAMENTO);
        match.setAtivo(true);

        when(resultRepository.existsByMatchId(20L)).thenReturn(false);
        when(resultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> {
            MatchResult result = invocation.getArgument(0);
            result.setId(30L);
            return result;
        });

        MatchResultDTO result = service.criarAutomaticoSumo(match, winner, 2, 0);

        assertEquals(30L, result.getId());
        assertEquals(20L, result.getMatchId());
        assertEquals(7L, result.getWinnerRegistrationId());
        assertEquals(2, result.getPontosA());
        assertEquals(0, result.getPontosB());
        assertEquals(StatusMatch.FINALIZADA, match.getStatus());
        verify(matchRepository).save(match);
        verify(bracketProgressionService).avancarVencedor(match, winner);
    }

    private Match matchDaModalidade(Modalidade modalidade) {
        CompetitionCategory category = mock(CompetitionCategory.class);
        Bracket bracket = mock(Bracket.class);
        Match match = mock(Match.class);

        when(bracket.getAtivo()).thenReturn(true);
        when(bracket.getAtual()).thenReturn(true);
        when(category.getModalidade()).thenReturn(modalidade);
        when(bracket.getCategory()).thenReturn(category);
        when(match.getBracket()).thenReturn(bracket);
        return match;
    }

    private MatchResultDTO dto(Long matchId) {
        MatchResultDTO dto = new MatchResultDTO();
        dto.setMatchId(matchId);
        dto.setWinnerRegistrationId(10L);
        dto.setPontosA(2);
        dto.setPontosB(0);
        return dto;
    }
}
