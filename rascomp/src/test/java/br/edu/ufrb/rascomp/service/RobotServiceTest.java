package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class RobotServiceTest {

    @Mock private RobotRepository robotRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    private RobotService service;

    @Test
    void naoDeveRemoverRoboComInscricaoAtiva() {
        Robot robot = robot(5L);
        when(robotRepository.findById(5L)).thenReturn(Optional.of(robot));
        when(registrationRepository.findByRobotIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(new Registration()));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletar(5L));

        assertEquals(true, ex.getMessage().contains("inscrição PENDENTE ou APROVADA"));
    }

    @Test
    void deveDesativarCadastroSemInscricaoAtiva() {
        Robot robot = robot(5L);
        when(robotRepository.findById(5L)).thenReturn(Optional.of(robot));
        when(registrationRepository.findByRobotIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());
        when(robotRepository.save(any(Robot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deletar(5L);

        assertEquals(false, robot.getAtivo());
        verify(robotRepository).save(robot);
    }

    private Robot robot(Long id) {
        Team team = new Team();
        team.setId(3L);

        Robot robot = new Robot();
        robot.setId(id);
        robot.setNome("Vespa");
        robot.setTeam(team);
        robot.setAtivo(true);
        return robot;
    }
}
