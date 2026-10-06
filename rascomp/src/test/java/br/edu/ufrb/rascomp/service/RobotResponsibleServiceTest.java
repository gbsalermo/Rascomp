package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;

@ExtendWith(MockitoExtension.class)
class RobotResponsibleServiceTest {

    @Mock private AccessPolicyService accessPolicyService;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private RobotResponsibleRepository responsibleRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private RegistrationCompositionService compositionService;

    @InjectMocks
    private RobotResponsibleService service;

    @Test
    void criadorDoRoboDeveVirarResponsavelInicial() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setNome("Gabriel");
        user.setEmail("gabriel@rascomp.local");
        user.setAtivo(true);

        Institution institution = new Institution();
        institution.setId(2L);
        institution.setNome("UFRB");

        Team team = new Team();
        team.setId(3L);
        team.setInstitution(institution);
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(4L);
        competitor.setTeam(team);
        competitor.setUserAccount(user);
        competitor.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(5L);
        robot.setNome("Vespa");
        robot.setTeam(team);
        robot.setAtivo(true);

        when(robotRepository.findById(5L)).thenReturn(Optional.of(robot));
        when(competitorRepository.findByUserAccountId(1L)).thenReturn(Optional.of(competitor));
        when(responsibleRepository.findByRobotIdAndCompetitorId(5L, 4L))
                .thenReturn(Optional.empty());
        when(responsibleRepository.save(any(RobotResponsible.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.associarCriador(5L, user);

        ArgumentCaptor<RobotResponsible> captor = ArgumentCaptor.forClass(RobotResponsible.class);
        org.mockito.Mockito.verify(responsibleRepository).save(captor.capture());

        assertEquals(5L, captor.getValue().getRobot().getId());
        assertEquals(4L, captor.getValue().getCompetitor().getId());
        assertEquals(true, captor.getValue().getAtivo());
    }


    @Test
    void liderPodeAlterarResponsaveisAntesDaCompeticaoESincronizaComposicao() {
        UserAccount leader = new UserAccount();
        leader.setId(1L);
        leader.setNome("Líder");
        leader.setAtivo(true);

        Institution institution = new Institution();
        institution.setId(2L);
        institution.setNome("UFRB");

        Team team = new Team();
        team.setId(3L);
        team.setInstitution(institution);
        team.setResponsibleUser(leader);
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(4L);
        competitor.setNome("Gabriel");
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(5L);
        robot.setNome("Vespa");
        robot.setTeam(team);
        robot.setAtivo(true);

        RobotResponsible link = new RobotResponsible();
        link.setId(6L);
        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setAtivo(true);

        when(accessPolicyService.exigirRoboDaEquipe(5L)).thenReturn(robot);
        when(accessPolicyService.usuarioAtual()).thenReturn(leader);
        when(robotRepository.findById(5L)).thenReturn(Optional.of(robot));
        when(registrationRepository.findByRobotIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());
        when(responsibleRepository.findByRobotIdOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of(link));
        when(responsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of());

        service.definir(5L, Set.of());

        assertEquals(false, link.getAtivo());
        org.mockito.Mockito.verify(compositionService)
                .sinalizarMudancaResponsabilidade(
                        5L,
                        4L,
                        RegistrationCompetitorChangeType.REMOVIDO,
                        leader);
        org.mockito.Mockito.verify(compositionService)
                .sincronizarRobot(5L, leader, false);
    }

}
