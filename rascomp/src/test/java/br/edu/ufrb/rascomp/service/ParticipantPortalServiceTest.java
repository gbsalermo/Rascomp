package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;

@ExtendWith(MockitoExtension.class)
class ParticipantPortalServiceTest {

    @Mock private AccessPolicyService accessPolicyService;
    @Mock private TeamService teamService;
    @Mock private CompetitorService competitorService;
    @Mock private RobotService robotService;
    @Mock private RegistrationService registrationService;
    @Mock private RegistrationCancellationRequestService cancellationRequestService;
    @Mock private RobotImageService robotImageService;
    @Mock private TentativaSeguidorLinhaService tentativaSeguidorLinhaService;
    @Mock private ConfigFollowService configFollowService;

    @InjectMocks
    private ParticipantPortalService service;

    @Test
    void liderDeveVisualizarTodosOsRobosDaEquipe() {
        UserAccount lider = usuario(1L);
        Team team = equipe(10L, lider);

        RobotDTO chronos = robo(100L, "Chronos");
        RobotDTO titan = robo(101L, "Titan");

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(lider);
        when(accessPolicyService.ehResponsavel(team, lider)).thenReturn(true);
        when(robotService.listarPorEquipe(10L, false)).thenReturn(List.of(chronos, titan));

        List<RobotDTO> resultado = service.robos(10L);

        assertEquals(List.of(100L, 101L), resultado.stream().map(RobotDTO::getId).toList());
        verify(registrationService, never()).listarPorEquipeEParticipante(10L, 1L);
    }

    @Test
    void membroDeveVisualizarSomenteRobosDasPropriasInscricoes() {
        UserAccount lider = usuario(1L);
        UserAccount membro = usuario(2L);
        Team team = equipe(10L, lider);

        RegistrationDTO follow = inscricao(200L, 100L);
        RegistrationDTO sumoMesmoRobo = inscricao(201L, 100L);
        RegistrationDTO outroRobo = inscricao(202L, 102L);

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(membro);
        when(accessPolicyService.ehResponsavel(team, membro)).thenReturn(false);
        when(registrationService.listarPorEquipeEParticipante(10L, 2L))
                .thenReturn(List.of(follow, sumoMesmoRobo, outroRobo));
        when(robotService.buscarPorId(100L)).thenReturn(robo(100L, "Chronos"));
        when(robotService.buscarPorId(102L)).thenReturn(robo(102L, "Ares"));

        List<RobotDTO> resultado = service.robos(10L);

        assertEquals(List.of(100L, 102L), resultado.stream().map(RobotDTO::getId).toList());
        verify(robotService, never()).listarPorEquipe(10L, false);
    }

    @Test
    void membroDeveReceberSomenteSuasInscricoes() {
        UserAccount lider = usuario(1L);
        UserAccount membro = usuario(2L);
        Team team = equipe(10L, lider);
        RegistrationDTO propria = inscricao(200L, 100L);

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(membro);
        when(accessPolicyService.ehResponsavel(team, membro)).thenReturn(false);
        when(registrationService.listarPorEquipeEParticipante(10L, 2L))
                .thenReturn(List.of(propria));

        List<RegistrationDTO> resultado = service.inscricoes(10L);

        assertEquals(List.of(200L), resultado.stream().map(RegistrationDTO::getId).toList());
        verify(registrationService, never()).listarPorEquipe(10L, false);
    }

    private UserAccount usuario(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setAtivo(true);
        return user;
    }

    private Team equipe(Long id, UserAccount responsavel) {
        Team team = new Team();
        team.setId(id);
        team.setResponsibleUser(responsavel);
        team.setAtivo(true);
        return team;
    }

    private RobotDTO robo(Long id, String nome) {
        RobotDTO dto = new RobotDTO();
        dto.setId(id);
        dto.setNome(nome);
        dto.setTeamId(10L);
        dto.setAtivo(true);
        return dto;
    }

    private RegistrationDTO inscricao(Long id, Long robotId) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(id);
        dto.setTeamId(10L);
        dto.setRobotId(robotId);
        return dto;
    }
}
