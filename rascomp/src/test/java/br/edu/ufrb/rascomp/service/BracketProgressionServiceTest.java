package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;

@ExtendWith(MockitoExtension.class)
class BracketProgressionServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private RoundSumoRepository roundSumoRepository;
    @Mock private BracketRepository bracketRepository;

    @InjectMocks
    private BracketProgressionService service;

    @Test
    void deveTrocarVencedorPropagadoQuandoProximaPartidaNaoComecou() {
        Registration anterior = registration(1L);
        Registration novo = registration(2L);
        Registration outroLado = registration(3L);
        Bracket bracket = bracket(10L, StatusBracket.EM_ANDAMENTO);
        Match origem = origem(bracket, anterior, novo);
        Match proxima = proxima(bracket, anterior, outroLado);

        when(matchRepository.findByBracketIdAndRodadaAndOrdem(10L, 2, 1))
                .thenReturn(Optional.of(proxima));

        service.corrigirVencedor(origem, anterior, novo);

        assertSame(novo, proxima.getRegistrationA());
        assertSame(outroLado, proxima.getRegistrationB());
        assertEquals(StatusMatch.AGENDADA, proxima.getStatus());
        verify(matchRepository).save(proxima);
    }

    @Test
    void deveBloquearCorrecaoQuandoProximaPartidaJaPossuiRound() {
        Registration anterior = registration(1L);
        Registration novo = registration(2L);
        Registration outroLado = registration(3L);
        Bracket bracket = bracket(10L, StatusBracket.EM_ANDAMENTO);
        Match origem = origem(bracket, anterior, novo);
        Match proxima = proxima(bracket, anterior, outroLado);

        when(matchRepository.findByBracketIdAndRodadaAndOrdem(10L, 2, 1))
                .thenReturn(Optional.of(proxima));
        when(roundSumoRepository.existsByMatchId(20L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.corrigirVencedor(origem, anterior, novo));

        assertSame(anterior, proxima.getRegistrationA());
    }

    @Test
    void deveRemoverVencedorPropagadoEVoltarProximaPartidaParaAguardando() {
        Registration anterior = registration(1L);
        Registration adversarioOrigem = registration(2L);
        Registration outroLado = registration(3L);
        Bracket bracket = bracket(10L, StatusBracket.EM_ANDAMENTO);
        Match origem = origem(bracket, anterior, adversarioOrigem);
        Match proxima = proxima(bracket, anterior, outroLado);
        proxima.setStatusConvocacao(StatusConvocacaoPartida.CONVOCADA);

        when(matchRepository.findByBracketIdAndRodadaAndOrdem(10L, 2, 1))
                .thenReturn(Optional.of(proxima));

        service.corrigirVencedor(origem, anterior, null);

        assertNull(proxima.getRegistrationA());
        assertEquals(StatusMatch.AGUARDANDO_PARTICIPANTES, proxima.getStatus());
        assertEquals(StatusConvocacaoPartida.NAO_CONVOCADA, proxima.getStatusConvocacao());
        verify(matchRepository).save(proxima);
    }

    @Test
    void removerResultadoDaFinalDeveReabrirChaveamento() {
        Registration anterior = registration(1L);
        Registration adversario = registration(2L);
        Bracket bracket = bracket(10L, StatusBracket.FINALIZADO);
        Match finalMatch = origem(bracket, anterior, adversario);

        when(matchRepository.findByBracketIdAndRodadaAndOrdem(10L, 2, 1))
                .thenReturn(Optional.empty());
        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));

        service.corrigirVencedor(finalMatch, anterior, null);

        assertEquals(StatusBracket.EM_ANDAMENTO, bracket.getStatus());
        verify(bracketRepository).save(bracket);
    }

    private Match origem(Bracket bracket, Registration a, Registration b) {
        Match match = new Match();
        match.setId(11L);
        match.setBracket(bracket);
        match.setRodada(1);
        match.setOrdem(1);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        match.setStatus(StatusMatch.FINALIZADA);
        return match;
    }

    private Match proxima(Bracket bracket, Registration a, Registration b) {
        Match match = new Match();
        match.setId(20L);
        match.setBracket(bracket);
        match.setRodada(2);
        match.setOrdem(1);
        match.setRegistrationA(a);
        match.setRegistrationB(b);
        match.setStatus(StatusMatch.AGENDADA);
        return match;
    }

    private Bracket bracket(Long id, StatusBracket status) {
        Bracket bracket = new Bracket();
        bracket.setId(id);
        bracket.setStatus(status);
        return bracket;
    }

    private Registration registration(Long id) {
        Registration registration = new Registration();
        registration.setId(id);
        return registration;
    }
}
