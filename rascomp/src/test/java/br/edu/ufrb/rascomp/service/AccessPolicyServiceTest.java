package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceTest {

    @Mock private UserAccountService userAccountService;
    @Mock private TeamRepository teamRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private RobotResponsibleRepository robotResponsibleRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    private AccessPolicyService service;

    @Test
    void responsavelPodeAcessarSuaEquipe() {
        UserAccount user = usuario(5L);
        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(user);

        when(userAccountService.buscarAtual()).thenReturn(user);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));

        assertEquals(9L, service.exigirEquipeDoResponsavel(9L).getId());
    }

    @Test
    void membroPodeAcessarEquipeSemSerResponsavel() {
        UserAccount atual = usuario(5L);
        UserAccount responsavel = usuario(6L);

        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(responsavel);

        Competitor competitor = new Competitor();
        competitor.setId(20L);
        competitor.setUserAccount(atual);
        competitor.setTeam(team);
        competitor.setAtivo(true);

        when(userAccountService.buscarAtual()).thenReturn(atual);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));
        when(competitorRepository.findByUserAccountId(5L)).thenReturn(Optional.of(competitor));

        assertEquals(9L, service.exigirEquipeDoParticipante(9L).getId());
    }

    @Test
    void criadorDoRoboPodeGerenciarInscricaoMesmoSemSerLider() {
        UserAccount atual = usuario(5L);
        UserAccount lider = usuario(6L);

        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(lider);

        Competitor competitor = new Competitor();
        competitor.setId(20L);
        competitor.setUserAccount(atual);
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(30L);
        robot.setTeam(team);
        robot.setCreatedByUser(atual);
        robot.setAtivo(true);

        Registration registration = new Registration();
        registration.setId(40L);
        registration.setTeam(team);
        registration.setRobot(robot);

        when(userAccountService.buscarAtual()).thenReturn(atual);
        when(registrationRepository.findById(40L)).thenReturn(Optional.of(registration));
        assertEquals(40L, service.exigirInscricaoGerenciavelPeloParticipante(40L).getId());
    }

    @Test
    void membroSemResponsabilidadePeloRoboNaoPodeGerenciarInscricao() {
        UserAccount atual = usuario(5L);
        UserAccount lider = usuario(6L);

        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(lider);

        Competitor competitor = new Competitor();
        competitor.setId(20L);
        competitor.setUserAccount(atual);
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(30L);
        robot.setTeam(team);
        robot.setAtivo(true);

        Registration registration = new Registration();
        registration.setId(40L);
        registration.setTeam(team);
        registration.setRobot(robot);

        when(userAccountService.buscarAtual()).thenReturn(atual);
        when(registrationRepository.findById(40L)).thenReturn(Optional.of(registration));

        assertThrows(AccessDeniedException.class,
                () -> service.exigirInscricaoGerenciavelPeloParticipante(40L));
    }

    @Test
    void participanteNaoPodeAcessarEquipeDeOutroResponsavel() {
        UserAccount atual = usuario(5L);
        UserAccount outro = usuario(6L);
        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(outro);

        when(userAccountService.buscarAtual()).thenReturn(atual);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));

        assertThrows(AccessDeniedException.class, () -> service.exigirEquipeDoResponsavel(9L));
    }

    private UserAccount usuario(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setAtivo(true);
        return user;
    }
}
