package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private BracketRepository bracketRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private InspecaoSumoService inspecaoSumoService;

    @InjectMocks
    private MatchService service;

    @Test
    void deveRejeitarPartidaEmBracketFollowLine() {
        CompetitionCategory follow = CompetitionCategory.builder()
                .id(3L)
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCategory(follow);
        bracket.setAtivo(true);

        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));

        MatchDTO dto = new MatchDTO();
        dto.setBracketId(10L);
        dto.setRodada(1);
        dto.setOrdem(1);
        dto.setRegistrationAId(1L);

        Registration registration = new Registration();
        registration.setId(1L);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(matchRepository, inspecaoSumoService);
    }

    @Test
    void deveRejeitarParticipanteSumoNaoApto() {
        Competition competition = new Competition();
        competition.setId(1L);

        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(1L)
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        Bracket bracket = new Bracket();
        bracket.setId(10L);
        bracket.setCompetition(competition);
        bracket.setCategory(sumo);
        bracket.setAtivo(true);

        Team team = new Team();
        team.setId(1L);

        Robot robot = new Robot();
        robot.setId(1L);
        robot.setTeam(team);

        Registration registration = new Registration();
        registration.setId(1L);
        registration.setCompetition(competition);
        registration.setCategory(sumo);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setAtivo(true);
        registration.setStatus(StatusRegistration.APROVADA);

        when(bracketRepository.findById(10L)).thenReturn(Optional.of(bracket));
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(inspecaoSumoService.estaAptaParaCompetir(1L)).thenReturn(false);

        MatchDTO dto = new MatchDTO();
        dto.setBracketId(10L);
        dto.setRodada(1);
        dto.setOrdem(1);
        dto.setRegistrationAId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(matchRepository);
    }
}
