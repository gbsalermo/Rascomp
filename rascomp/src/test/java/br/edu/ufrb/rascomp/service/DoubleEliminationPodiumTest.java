package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.*;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.*;

@ExtendWith(MockitoExtension.class)
class DoubleEliminationPodiumTest {
    @Mock RegistrationRepository registrationRepository;
    @Mock CompetitionRepository competitionRepository;
    @Mock BracketRepository bracketRepository;
    @Mock MatchRepository matchRepository;
    @Mock MatchResultRepository matchResultRepository;
    @Mock RankingFollowService rankingFollowService;
    @Mock FollowResolutionService followResolutionService;
    @Mock FollowManualResultRepository followManualResultRepository;
    @Mock CompetitionContextService competitionContextService;
    @InjectMocks CompetitionResultsService service;

    private Registration robot(long id, Competition competition, CompetitionCategory category) {
        Robot robot = new Robot();
        robot.setId(id);
        robot.setNome("Robô " + id);
        Team team = new Team();
        team.setId(id);
        team.setNome("Equipe " + id);
        Registration registration = new Registration();
        registration.setId(id);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setRobot(robot);
        registration.setTeam(team);
        return registration;
    }

    private Match match(long id, Bracket bracket, int round, String section,
                        Registration a, Registration b) {
        Match match = new Match();
        match.setId(id);
        match.setBracket(bracket);
        match.setRodada(round);
        match.setOrdem(1);
        match.setBracketSection(section);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        return match;
    }

    private MatchResult result(Match match, Registration winner) {
        MatchResult result = new MatchResult();
        result.setMatch(match);
        result.setWinner(winner);
        result.setPontosA(2);
        result.setPontosB(1);
        return result;
    }

    @Test
    void podioAguardaResetQuandoVencedorDaLosersVencePrimeiraFinal() {
        Competition competition = new Competition();
        competition.setId(1L);
        CompetitionCategory category = CompetitionCategory.builder().id(2L).nome("Mini Sumô")
                .modalidade(Modalidade.SUMO).ativo(true).build();
        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCompetition(competition);
        bracket.setCategory(category);
        bracket.setBracketFormat("DOUBLE_ELIMINATION");
        bracket.setAtivo(true);
        bracket.setAtual(true);

        Registration invicto = robot(1L, competition, category);
        Registration challenger = robot(2L, competition, category);
        Registration terceiro = robot(3L, competition, category);
        Match losers = match(20L, bracket, 4, "LOSERS", challenger, terceiro);
        Match grand = match(30L, bracket, 5, "GRAND_FINAL", invicto, challenger);
        Match reset = match(31L, bracket, 6, "GRAND_FINAL_RESET", challenger, invicto);

        when(registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(1L))
                .thenReturn(List.of(invicto, challenger, terceiro));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(1L, 2L))
                .thenReturn(List.of(bracket));
        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L))
                .thenReturn(List.of(losers, grand, reset));
        when(matchResultRepository.findByMatchId(30L)).thenReturn(Optional.of(result(grand, challenger)));
        when(matchResultRepository.findByMatchId(31L))
                .thenReturn(Optional.empty(), Optional.of(result(reset, invicto)));
        when(matchResultRepository.findByMatchId(20L)).thenReturn(Optional.of(result(losers, challenger)));

        var pending = service.listar(1L).get(0);
        assertEquals("PARCIAL", pending.getStatus());
        assertNull(pending.getWinnerRegistrationId());

        var podium = service.listar(1L).get(0);
        assertEquals("CONCLUIDO", podium.getStatus());
        assertEquals(1L, podium.getWinnerRegistrationId());
        assertEquals(2L, podium.getSecondRegistrationId());
        assertEquals(3L, podium.getThirdRegistrationId());
        assertEquals(31L, podium.getFinalMatchId());
    }
}
