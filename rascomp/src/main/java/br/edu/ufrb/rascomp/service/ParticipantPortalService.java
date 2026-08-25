package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitorRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRobotRequest;
import br.edu.ufrb.rascomp.dto.ParticipantTeamRequest;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantPortalService {

    private final AccessPolicyService accessPolicyService;
    private final TeamService teamService;
    private final CompetitorService competitorService;
    private final RobotService robotService;
    private final RegistrationService registrationService;
    private final RobotImageService robotImageService;

    @Transactional(readOnly = true)
    public List<TeamDTO> minhasEquipes() {
        UserAccount usuario = accessPolicyService.usuarioAtual();
        return teamService.listarPorResponsavel(usuario.getId());
    }

    @Transactional
    public TeamDTO criarEquipe(ParticipantTeamRequest request) {
        TeamDTO dto = teamDto(request);
        return teamService.criarParaResponsavel(dto, accessPolicyService.usuarioAtual());
    }

    @Transactional
    public TeamDTO atualizarEquipe(Long teamId, ParticipantTeamRequest request) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        TeamDTO dto = teamDto(request);
        return teamService.atualizarComoResponsavel(teamId, dto, accessPolicyService.usuarioAtual());
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> competidores(Long teamId) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return competitorService.listarPorEquipe(teamId, false);
    }

    @Transactional
    public CompetitorDTO criarCompetidor(Long teamId, ParticipantCompetitorRequest request) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        CompetitorDTO dto = competitorDto(request, teamId);
        return competitorService.criar(dto);
    }

    @Transactional
    public CompetitorDTO tornarMeCompetidor(Long teamId) {
        Team team = accessPolicyService.exigirEquipeDoResponsavel(teamId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        CompetitorDTO dto = new CompetitorDTO();
        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setTelefone(usuario.getTelefone());
        dto.setTeamId(team.getId());
        dto.setUserAccountId(usuario.getId());
        dto.setAtivo(true);
        return competitorService.criar(dto);
    }

    @Transactional
    public CompetitorDTO atualizarCompetidor(Long competitorId, ParticipantCompetitorRequest request) {
        Competitor atual = accessPolicyService.exigirCompetidorDaEquipe(competitorId);
        CompetitorDTO dto = competitorDto(request, atual.getTeam().getId());
        dto.setUserAccountId(atual.getUserAccount() == null ? null : atual.getUserAccount().getId());
        dto.setAtivo(atual.getAtivo());
        return competitorService.atualizar(competitorId, dto);
    }

    @Transactional
    public void removerCompetidor(Long competitorId) {
        accessPolicyService.exigirCompetidorDaEquipe(competitorId);
        competitorService.deletar(competitorId);
    }

    @Transactional(readOnly = true)
    public List<RobotDTO> robos(Long teamId) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return robotService.listarPorEquipe(teamId, false);
    }

    @Transactional
    public RobotDTO criarRobo(Long teamId, ParticipantRobotRequest request) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        RobotDTO dto = robotDto(request, teamId);
        return robotService.criar(dto);
    }

    @Transactional
    public RobotDTO atualizarRobo(Long robotId, ParticipantRobotRequest request) {
        Robot atual = accessPolicyService.exigirRoboDaEquipe(robotId);
        RobotDTO dto = robotDto(request, atual.getTeam().getId());
        dto.setAtivo(atual.getAtivo());
        return robotService.atualizar(robotId, dto);
    }

    @Transactional
    public void removerRobo(Long robotId) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        robotService.deletar(robotId);
    }

    public List<RobotImageDTO> fotos(Long robotId) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        return robotImageService.listar(robotId);
    }

    public RobotImageDTO adicionarFoto(Long robotId, MultipartFile arquivo) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        return robotImageService.adicionar(robotId, arquivo);
    }

    public RobotImageDTO definirFotoPrincipal(Long robotId, Long imageId) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        return robotImageService.definirPrincipal(robotId, imageId);
    }

    public void removerFoto(Long robotId, Long imageId) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        robotImageService.remover(robotId, imageId);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> inscricoes(Long teamId) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return registrationService.listarPorEquipe(teamId, false);
    }

    @Transactional
    public RegistrationDTO inscrever(Long teamId, ParticipantRegistrationRequest request) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        Robot robot = accessPolicyService.exigirRoboDaEquipe(request.getRobotId());
        if (!robot.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("O robô deve pertencer à equipe informada.");
        }

        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(request.getCompetitionId());
        dto.setCategoryId(request.getCategoryId());
        dto.setTeamId(teamId);
        dto.setRobotId(request.getRobotId());
        dto.setCompetitorIds(request.getCompetitorIds());
        dto.setObservacao(request.getObservacao());
        return registrationService.criarPorParticipante(dto, accessPolicyService.usuarioAtual());
    }

    @Transactional
    public void cancelarInscricao(Long registrationId) {
        accessPolicyService.exigirInscricaoDaEquipe(registrationId);
        registrationService.deletar(registrationId);
    }

    private TeamDTO teamDto(ParticipantTeamRequest request) {
        TeamDTO dto = new TeamDTO();
        dto.setNome(request.getNome());
        dto.setInstitutionId(request.getInstitutionId());
        return dto;
    }

    private CompetitorDTO competitorDto(ParticipantCompetitorRequest request, Long teamId) {
        CompetitorDTO dto = new CompetitorDTO();
        dto.setNome(request.getNome());
        dto.setEmail(request.getEmail());
        dto.setTelefone(request.getTelefone());
        dto.setTeamId(teamId);
        dto.setAtivo(true);
        return dto;
    }

    private RobotDTO robotDto(ParticipantRobotRequest request, Long teamId) {
        RobotDTO dto = new RobotDTO();
        dto.setNome(request.getNome());
        dto.setDescricao(request.getDescricao());
        dto.setTeamId(teamId);
        dto.setAtivo(true);
        return dto;
    }
}
