package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
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
    public TeamDTO criarEquipe(TeamDTO dto) {
        return teamService.criarParaResponsavel(dto, accessPolicyService.usuarioAtual());
    }

    @Transactional
    public TeamDTO atualizarEquipe(Long teamId, TeamDTO dto) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return teamService.atualizarComoResponsavel(teamId, dto, accessPolicyService.usuarioAtual());
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> competidores(Long teamId) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return competitorService.listarPorEquipe(teamId, false);
    }

    @Transactional
    public CompetitorDTO criarCompetidor(Long teamId, CompetitorDTO dto) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        dto.setTeamId(teamId);
        dto.setUserAccountId(null);
        dto.setAtivo(true);
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
    public CompetitorDTO atualizarCompetidor(Long competitorId, CompetitorDTO dto) {
        Competitor atual = accessPolicyService.exigirCompetidorDaEquipe(competitorId);
        dto.setTeamId(atual.getTeam().getId());
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
    public RobotDTO criarRobo(Long teamId, RobotDTO dto) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        dto.setTeamId(teamId);
        dto.setAtivo(true);
        return robotService.criar(dto);
    }

    @Transactional
    public RobotDTO atualizarRobo(Long robotId, RobotDTO dto) {
        Robot atual = accessPolicyService.exigirRoboDaEquipe(robotId);
        dto.setTeamId(atual.getTeam().getId());
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
    public RegistrationDTO inscrever(Long teamId, RegistrationDTO dto) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        accessPolicyService.exigirRoboDaEquipe(dto.getRobotId());
        dto.setTeamId(teamId);
        dto.setStatus(null);
        dto.setAtivo(true);
        return registrationService.criarPorParticipante(dto, accessPolicyService.usuarioAtual());
    }

    @Transactional
    public void cancelarInscricao(Long registrationId) {
        accessPolicyService.exigirInscricaoDaEquipe(registrationId);
        registrationService.deletar(registrationId);
    }
}
