package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionResultsServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private BracketRepository bracketRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private RankingFollowService rankingFollowService;
    @Mock private FollowResolutionService followResolutionService;
    @Mock private FollowManualResultRepository followManualResultRepository;
    @Mock private CompetitionContextService competitionContextService;

    @InjectMocks private CompetitionResultsService service;

    @Test
    void competicaoFinalizadaContinuaDisponivelParaConsultaPublica() {
        Competition competition = new Competition();
        competition.setId(3L);
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.FINALIZADA);
        competition.setVigente(false);
        when(competitionRepository.findById(3L)).thenReturn(Optional.of(competition));
        when(registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(3L))
                .thenReturn(List.of());

        assertEquals(List.of(), service.listarPublico(3L));
    }

    @Test
    void competicaoCanceladaNaoDisponibilizaResultadosPublicos() {
        Competition competition = new Competition();
        competition.setId(4L);
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.CANCELADA);
        when(competitionRepository.findById(4L)).thenReturn(Optional.of(competition));

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.listarPublico(4L));
    }
}
