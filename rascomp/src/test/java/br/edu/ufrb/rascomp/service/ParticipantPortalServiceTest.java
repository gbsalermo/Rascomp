package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ParticipantRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantTeamRequest;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;

@ExtendWith(MockitoExtension.class)
class ParticipantPortalServiceTest {

    @Mock private AccessPolicyService accessPolicyService;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private InstitutionService institutionService;
    @Mock private TeamService teamService;
    @Mock private CompetitorService competitorService;
    @Mock private RobotService robotService;
    @Mock private RobotResponsibleService robotResponsibleService;
    @Mock private RegistrationService registrationService;
    @Mock private RegistrationCancellationRequestService cancellationRequestService;
    @Mock private RobotImageService robotImageService;
    @Mock private TentativaSeguidorLinhaService tentativaSeguidorLinhaService;
    @Mock private ConfigFollowService configFollowService;

    @InjectMocks
    private ParticipantPortalService service;

    @Test
    void criarEquipeDeveVincularParticipanteComoCompetidorAutomaticamente() {
        UserAccount participante = usuario(1L);
        participante.setRole(UserRole.PARTICIPANTE);
        participante.setNome("Gabriel");
        participante.setEmail("gabriel@teste.com");

        ParticipantTeamRequest request = new ParticipantTeamRequest();
        request.setNome("Equipe Teste");
        request.setInstitutionId(5L);

        br.edu.ufrb.rascomp.dto.TeamDTO team = new br.edu.ufrb.rascomp.dto.TeamDTO();
        team.setId(10L);
        team.setNome("Equipe Teste");

        when(accessPolicyService.usuarioAtual()).thenReturn(participante);
        when(competitorRepository.findByUserAccountId(1L)).thenReturn(Optional.empty());
        when(teamService.criarParaResponsavel(any(br.edu.ufrb.rascomp.dto.TeamDTO.class), org.mockito.ArgumentMatchers.eq(participante)))
                .thenReturn(team);
        when(competitorService.criar(any(CompetitorDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.criarEquipe(request);

        assertEquals(10L, result.getId());
        verify(competitorService).criar(org.mockito.ArgumentMatchers.argThat(dto ->
                Long.valueOf(10L).equals(dto.getTeamId())
                        && Long.valueOf(1L).equals(dto.getUserAccountId())
                        && "gabriel@teste.com".equals(dto.getEmail())));
    }

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
    void membroDeveVisualizarSomenteRobosPelosQuaisEhResponsavel() {
        UserAccount lider = usuario(1L);
        UserAccount membro = usuario(2L);
        Team team = equipe(10L, lider);

        RobotDTO chronos = robo(100L, "Chronos");
        RobotDTO ares = robo(102L, "Ares");

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(membro);
        when(accessPolicyService.ehResponsavel(team, membro)).thenReturn(false);
        when(robotResponsibleService.listarRobosDoCompetidorAtual(10L))
                .thenReturn(List.of(chronos, ares));

        List<RobotDTO> resultado = service.robos(10L);

        assertEquals(List.of(100L, 102L), resultado.stream().map(RobotDTO::getId).toList());
        verify(robotService, never()).listarPorEquipe(10L, false);
        verify(registrationService, never()).listarPorEquipeEParticipante(10L, 2L);
    }

    @Test
    void membroDeveReceberInscricoesComoCompetidorEOuResponsavelPeloRoboSemDuplicar() {
        UserAccount lider = usuario(1L);
        UserAccount membro = usuario(2L);
        Team team = equipe(10L, lider);
        RegistrationDTO comoCompetidor = inscricao(200L, 100L);
        RegistrationDTO comoResponsavel = inscricao(201L, 101L);
        RobotDTO roboGerenciado = robo(101L, "Ares");

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(membro);
        when(accessPolicyService.ehResponsavel(team, membro)).thenReturn(false);
        when(robotResponsibleService.listarRobosDoCompetidorAtual(10L))
                .thenReturn(List.of(roboGerenciado));
        when(registrationService.listarPorEquipeEParticipante(10L, 2L))
                .thenReturn(List.of(comoCompetidor));
        when(registrationService.listarPorEquipeERobos(10L, List.of(101L)))
                .thenReturn(List.of(comoResponsavel));

        List<RegistrationDTO> resultado = service.inscricoes(10L);

        assertEquals(List.of(200L, 201L), resultado.stream().map(RegistrationDTO::getId).toList());
        verify(registrationService, never()).listarPorEquipe(10L, false);
    }

    @Test
    void membroResponsavelPeloRoboDevePoderEnviarInscricaoPendente() {
        UserAccount lider = usuario(1L);
        UserAccount membro = usuario(2L);
        membro.setRole(UserRole.PARTICIPANTE);
        Team team = equipe(10L, lider);

        Robot robot = new Robot();
        robot.setId(100L);
        robot.setNome("Chronos");
        robot.setTeam(team);
        robot.setAtivo(true);

        ParticipantRegistrationRequest request = new ParticipantRegistrationRequest();
        request.setCompetitionId(20L);
        request.setCategoryId(30L);
        request.setRobotId(100L);
        request.setCompetitorIds(List.of(40L));

        RegistrationDTO criada = inscricao(300L, 100L);

        when(accessPolicyService.exigirEquipeDoParticipante(10L)).thenReturn(team);
        when(accessPolicyService.exigirRoboGerenciavelPeloParticipante(100L)).thenReturn(robot);
        when(accessPolicyService.usuarioAtual()).thenReturn(membro);
        when(registrationService.criarPorParticipante(any(RegistrationDTO.class), org.mockito.ArgumentMatchers.eq(membro)))
                .thenReturn(criada);

        RegistrationDTO resultado = service.inscrever(10L, request);

        assertEquals(300L, resultado.getId());
        verify(accessPolicyService, never()).exigirEquipeDoResponsavel(10L);
        verify(registrationService).criarPorParticipante(
                org.mockito.ArgumentMatchers.argThat(dto ->
                        Long.valueOf(20L).equals(dto.getCompetitionId())
                                && Long.valueOf(30L).equals(dto.getCategoryId())
                                && Long.valueOf(100L).equals(dto.getRobotId())
                                && dto.getCompetitorIds().equals(List.of(40L))),
                org.mockito.ArgumentMatchers.eq(membro));
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
