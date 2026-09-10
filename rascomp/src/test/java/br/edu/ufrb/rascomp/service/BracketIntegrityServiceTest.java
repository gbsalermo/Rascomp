package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;

@ExtendWith(MockitoExtension.class)
class BracketIntegrityServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private RoundSumoRepository roundSumoRepository;

    @InjectMocks
    private BracketIntegrityService service;

    @Test
    void somenteInscricoesEncerradasDevemPermitirGeracaoComum() {
        Competition competition = new Competition();
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        assertDoesNotThrow(() -> service.validarEstadoParaGeracao(competition));

        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        assertThrows(IllegalArgumentException.class, () -> service.validarEstadoParaGeracao(competition));

        competition.setStatus(StatusCompetition.EM_ANDAMENTO);
        assertThrows(IllegalArgumentException.class, () -> service.validarEstadoParaGeracao(competition));
    }

    @Test
    void byeAutomaticoNaoDeveContarComoAtividadeCompetitivaReal() {
        Bracket bracket = bracket(10L);
        Match byeFinalizado = new Match();
        byeFinalizado.setStatus(StatusMatch.FINALIZADA);
        byeFinalizado.setRegistrationA(registration(1L));
        byeFinalizado.setRegistrationB(null);

        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(10L))
                .thenReturn(List.of(byeFinalizado));

        assertFalse(service.possuiAtividadeCompetitiva(bracket));
        assertDoesNotThrow(() -> service.validarSemAtividadeCompetitiva(bracket));
    }

    @Test
    void roundRegistradoDeveBloquearRegeneracao() {
        Bracket bracket = bracket(11L);
        when(roundSumoRepository.existsByMatchBracketId(11L)).thenReturn(true);

        assertTrue(service.possuiAtividadeCompetitiva(bracket));
        assertThrows(IllegalArgumentException.class, () -> service.validarSemAtividadeCompetitiva(bracket));
    }

    @Test
    void partidaEmAndamentoDeveBloquearRegeneracaoMesmoSemResultado() {
        Bracket bracket = bracket(12L);
        Match iniciada = new Match();
        iniciada.setStatus(StatusMatch.EM_ANDAMENTO);

        when(matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(12L))
                .thenReturn(List.of(iniciada));

        assertTrue(service.possuiAtividadeCompetitiva(bracket));
    }

    private Bracket bracket(Long id) {
        Bracket bracket = new Bracket();
        bracket.setId(id);
        return bracket;
    }

    private Registration registration(Long id) {
        Registration registration = new Registration();
        registration.setId(id);
        return registration;
    }
}
