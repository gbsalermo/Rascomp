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

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.*;

@ExtendWith(MockitoExtension.class)
class DoubleEliminationProgressionTest {
    @Mock MatchRepository matchRepository;
    @Mock MatchResultRepository matchResultRepository;
    @Mock RoundSumoRepository roundSumoRepository;
    @Mock BracketRepository bracketRepository;
    @InjectMocks BracketProgressionService service;

    private Bracket bracket() {
        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setBracketFormat("DOUBLE_ELIMINATION");
        bracket.setStatus(StatusBracket.EM_ANDAMENTO);
        return bracket;
    }

    private Registration registration(long id) {
        Registration registration = new Registration();
        registration.setId(id);
        return registration;
    }

    private Match match(long id, Bracket bracket, int round, String section,
                        Registration a, Registration b, StatusMatch status) {
        Match m = new Match();
        m.setId(id);
        m.setBracket(bracket);
        m.setRodada(round);
        m.setOrdem(1);
        m.setBracketSection(section);
        m.setRegistrationA(a);
        m.setRegistrationB(b);
        m.setStatus(status);
        m.setAtivo(true);
        return m;
    }

    @Test
    void finalGeralVencidaPeloInvictoEncerraSemReset() {
        Bracket b = bracket();
        Registration undefeated = registration(1);
        Registration challenger = registration(2);
        Match grand = match(30, b, 5, "GRAND_FINAL", undefeated, challenger, StatusMatch.FINALIZADA);
        Match reset = match(31, b, 6, "GRAND_FINAL_RESET", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        grand.setWinnerNextMatch(reset);
        grand.setWinnerNextSlot("A");
        grand.setLoserNextMatch(reset);
        grand.setLoserNextSlot("B");

        when(bracketRepository.findById(10L)).thenReturn(Optional.of(b));
        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L)).thenReturn(List.of(grand, reset));

        service.avancarVencedor(grand, undefeated);

        assertEquals(StatusBracket.FINALIZADO, b.getStatus());
        assertNull(reset.getRegistrationA());
        assertNull(reset.getRegistrationB());
    }

    @Test
    void finalGeralVencidaPelaLosersAgendaResetComAmbosFinalistas() {
        Bracket b = bracket();
        Registration undefeated = registration(1);
        Registration challenger = registration(2);
        Match grand = match(30, b, 5, "GRAND_FINAL", undefeated, challenger, StatusMatch.FINALIZADA);
        Match reset = match(31, b, 6, "GRAND_FINAL_RESET", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        grand.setWinnerNextMatch(reset);
        grand.setWinnerNextSlot("A");
        grand.setLoserNextMatch(reset);
        grand.setLoserNextSlot("B");
        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L)).thenReturn(List.of(grand, reset));

        service.avancarVencedor(grand, challenger);

        assertEquals(StatusBracket.EM_ANDAMENTO, b.getStatus());
        assertSame(challenger, reset.getRegistrationA());
        assertSame(undefeated, reset.getRegistrationB());
        assertEquals(StatusMatch.AGENDADA, reset.getStatus());
    }

    @Test
    void perdedorDeByeNaoExisteEPrimeiraDerrotaFicaVivaNaLosers() {
        Bracket b = bracket();
        Registration a = registration(1);
        Registration p = registration(2);
        Registration q = registration(3);

        Match bye = match(1, b, 1, "WINNERS", a, null, StatusMatch.BYE);
        Match played = match(2, b, 1, "WINNERS", p, q, StatusMatch.AGENDADA);
        Match winnersFinal = match(3, b, 2, "WINNERS", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        Match losersOne = match(4, b, 3, "LOSERS", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        Match losersFinal = match(5, b, 4, "LOSERS", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);

        bye.setWinnerNextMatch(winnersFinal);
        bye.setWinnerNextSlot("A");
        bye.setLoserNextMatch(losersOne);
        bye.setLoserNextSlot("A");

        played.setWinnerNextMatch(winnersFinal);
        played.setWinnerNextSlot("B");
        played.setLoserNextMatch(losersOne);
        played.setLoserNextSlot("B");

        losersOne.setWinnerNextMatch(losersFinal);
        losersOne.setWinnerNextSlot("A");
        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L))
                .thenReturn(List.of(bye, played, winnersFinal, losersOne, losersFinal));

        service.avancarBye(bye);
        assertEquals(StatusMatch.AGUARDANDO_PARTICIPANTES, losersOne.getStatus());

        played.setStatus(StatusMatch.FINALIZADA);
        service.avancarVencedor(played, p);

        assertEquals(StatusMatch.FINALIZADA, losersOne.getStatus()); // BYE, no invented defeat
        assertSame(q, losersFinal.getRegistrationA());
        assertEquals(StatusMatch.AGENDADA, winnersFinal.getStatus());
        assertSame(a, winnersFinal.getRegistrationA());
        assertSame(p, winnersFinal.getRegistrationB());
    }

    @Test
    void correcaoDaFinalGeralAntesDoResetReabreEReconfiguraConfronto() {
        Bracket b = bracket();
        b.setStatus(StatusBracket.FINALIZADO);
        Registration a = registration(1);
        Registration challenger = registration(2);
        Match grand = match(30, b, 5, "GRAND_FINAL", a, challenger, StatusMatch.FINALIZADA);
        Match reset = match(31, b, 6, "GRAND_FINAL_RESET", null, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        grand.setWinnerNextMatch(reset);
        grand.setWinnerNextSlot("A");
        grand.setLoserNextMatch(reset);
        grand.setLoserNextSlot("B");
        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L)).thenReturn(List.of(grand, reset));
        when(bracketRepository.findById(10L)).thenReturn(Optional.of(b));

        service.corrigirVencedor(grand, a, challenger);

        assertEquals(StatusBracket.EM_ANDAMENTO, b.getStatus());
        assertSame(challenger, reset.getRegistrationA());
        assertSame(a, reset.getRegistrationB());
        assertEquals(StatusMatch.AGENDADA, reset.getStatus());
    }

    @Test
    void rejeitaCorrecaoQuandoPartidaDependenteJaTemRound() {
        Bracket b = bracket();
        Registration a = registration(1);
        Registration p = registration(2);
        Match source = match(1, b, 1, "WINNERS", a, p, StatusMatch.FINALIZADA);
        Match winnerTarget = match(2, b, 2, "WINNERS", a, null, StatusMatch.AGUARDANDO_PARTICIPANTES);
        Match loserTarget = match(3, b, 3, "LOSERS", null, p, StatusMatch.AGUARDANDO_PARTICIPANTES);
        source.setWinnerNextMatch(winnerTarget);
        source.setWinnerNextSlot("A");
        source.setLoserNextMatch(loserTarget);
        source.setLoserNextSlot("B");

        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L))
                .thenReturn(List.of(source, winnerTarget, loserTarget));
        when(roundSumoRepository.existsByMatchId(2L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.corrigirVencedor(source, a, p));
        assertSame(a, winnerTarget.getRegistrationA());
        assertSame(p, loserTarget.getRegistrationB());
    }

}
