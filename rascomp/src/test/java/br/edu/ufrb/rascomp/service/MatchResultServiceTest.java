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
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock private MatchResultRepository resultRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private BracketProgressionService bracketProgressionService;

    @InjectMocks
    private MatchResultService service;

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
