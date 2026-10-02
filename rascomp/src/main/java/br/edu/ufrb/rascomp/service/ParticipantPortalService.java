package br.edu.ufrb.rascomp.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ConfigFollowDTO;
import br.edu.ufrb.rascomp.dto.InstitutionDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitorRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRobotRequest;
import br.edu.ufrb.rascomp.dto.ParticipantTeamRequest;
import br.edu.ufrb.rascomp.dto.RegistrationCancellationRequestDTO;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.dto.RobotResponsibleDTO;
import br.edu.ufrb.rascomp.dto.RobotResponsibilityUpdateRequest;
import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantPortalService {

    private final AccessPolicyService accessPolicyService;
    private final CompetitorRepository competitorRepository;
    private final InstitutionService institutionService;
    private final TeamService teamService;
    private final CompetitorService competitorService;
    private final RobotService robotService;
    private final RobotResponsibleService robotResponsibleService;
    private final RegistrationService registrationService;
    private final RegistrationCancellationRequestService cancellationRequestService;
    private final RobotImageService robotImageService;
    private final TentativaSeguidorLinhaService tentativaSeguidorLinhaService;
    private final ConfigFollowService configFollowService;

    @Transactional(readOnly = true)
    public List<TeamDTO> minhasEquipes() {
        UserAccount usuario = accessPolicyService.usuarioAtual();
        return teamService.listarRelacionadasAoParticipante(usuario.getId());
    }

    @Transactional
    public TeamDTO criarEquipe(ParticipantTeamRequest request) {
        UserAccount usuario = accessPolicyService.usuarioAtual();

        competitorRepository.findByUserAccountId(usuario.getId()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Sua conta já está associada à equipe " + existing.getTeam().getNome()
                            + ". Um PARTICIPANTE deve possuir um único vínculo competitivo de equipe.");
        });

        TeamDTO dto = teamDto(request);
        TeamDTO team = teamService.criarParaResponsavel(dto, usuario);

        CompetitorDTO self = new CompetitorDTO();
        self.setNome(usuario.getNome());
        self.setEmail(usuario.getEmail());
        self.setTelefone(usuario.getTelefone());
        self.setTeamId(team.getId());
        self.setUserAccountId(usuario.getId());
        self.setAtivo(true);
        competitorService.criar(self);

        return team;
    }

    @Transactional
    public InstitutionDTO criarInstituicao(InstitutionDTO request) {
        UserAccount usuario = accessPolicyService.usuarioAtual();
        if (usuario.getRole() != br.edu.ufrb.rascomp.model.Enum.UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException("Somente PARTICIPANTE pode cadastrar instituição por este fluxo.");
        }
        request.setAtivo(true);
        return institutionService.criar(request);
    }

    @Transactional
    public TeamDTO atualizarEquipe(Long teamId, ParticipantTeamRequest request) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        TeamDTO dto = teamDto(request);
        return teamService.atualizarComoResponsavel(teamId, dto, accessPolicyService.usuarioAtual());
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> competidores(Long teamId) {
        accessPolicyService.exigirEquipeDoParticipante(teamId);
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
        Team team = accessPolicyService.exigirEquipeDoParticipante(teamId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        if (accessPolicyService.ehResponsavel(team, usuario)) {
            return robotService.listarPorEquipe(teamId, false);
        }

        return robotResponsibleService.listarRobosDoCompetidorAtual(teamId);
    }

    @Transactional
    public RobotDTO criarRobo(Long teamId, ParticipantRobotRequest request) {
        accessPolicyService.exigirEquipeDoParticipante(teamId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        RobotDTO dto = robotDto(request, teamId);
        RobotDTO criado = robotService.criar(dto, usuario);
        robotResponsibleService.associarCriador(criado.getId(), usuario);
        return criado;
    }

    @Transactional
    public RobotDTO atualizarRobo(Long robotId, ParticipantRobotRequest request) {
        Robot atual = accessPolicyService.exigirRoboGerenciavelPeloParticipante(robotId);
        RobotDTO dto = robotDto(request, atual.getTeam().getId());
        dto.setAtivo(atual.getAtivo());
        return robotService.atualizar(robotId, dto);
    }

    @Transactional(readOnly = true)
    public List<RobotResponsibleDTO> responsaveisRobo(Long robotId) {
        return robotResponsibleService.listar(robotId);
    }

    @Transactional
    public List<RobotResponsibleDTO> definirResponsaveisRobo(
            Long robotId,
            RobotResponsibilityUpdateRequest request) {
        return robotResponsibleService.definir(robotId, request.getCompetitorIds());
    }

    @Transactional
    public void removerRobo(Long robotId) {
        accessPolicyService.exigirRoboDaEquipe(robotId);
        robotService.deletar(robotId);
    }

    public List<RobotImageDTO> fotos(Long robotId) {
        accessPolicyService.exigirRoboVisivelAoParticipante(robotId);
        return robotImageService.listar(robotId);
    }

    public RobotImageDTO adicionarFoto(Long robotId, MultipartFile arquivo) {
        accessPolicyService.exigirRoboGerenciavelPeloParticipante(robotId);
        return robotImageService.adicionar(robotId, arquivo);
    }

    public RobotImageDTO definirFotoPrincipal(Long robotId, Long imageId) {
        accessPolicyService.exigirRoboGerenciavelPeloParticipante(robotId);
        return robotImageService.definirPrincipal(robotId, imageId);
    }

    public void removerFoto(Long robotId, Long imageId) {
        accessPolicyService.exigirRoboGerenciavelPeloParticipante(robotId);
        robotImageService.remover(robotId, imageId);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> inscricoes(Long teamId) {
        Team team = accessPolicyService.exigirEquipeDoParticipante(teamId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        if (accessPolicyService.ehResponsavel(team, usuario)) {
            return registrationService.listarPorEquipe(teamId, false);
        }

        List<Long> robotIds = robotResponsibleService.listarRobosDoCompetidorAtual(teamId)
                .stream()
                .map(RobotDTO::getId)
                .toList();

        List<RegistrationDTO> comoCompetidor =
                registrationService.listarPorEquipeEParticipante(teamId, usuario.getId());
        List<RegistrationDTO> comoResponsavelPeloRobo =
                registrationService.listarPorEquipeERobos(teamId, robotIds);

        Map<Long, RegistrationDTO> unicas = new LinkedHashMap<>();
        Stream.concat(comoCompetidor.stream(), comoResponsavelPeloRobo.stream())
                .forEach(registration -> unicas.putIfAbsent(registration.getId(), registration));

        return unicas.values().stream()
                .sorted(Comparator.comparing(
                        RegistrationDTO::getDataCadastro,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TentativaSeguidorLinhaDTO> tentativasFollow(Long registrationId) {
        accessPolicyService.exigirInscricaoVisivelAoParticipante(registrationId);
        return tentativaSeguidorLinhaService.listarPorInscricao(registrationId);
    }

    @Transactional(readOnly = true)
    public ConfigFollowDTO configFollow(Long registrationId) {
        Registration registration = accessPolicyService.exigirInscricaoVisivelAoParticipante(registrationId);
        return configFollowService.buscarPorCategoria(registration.getCategory().getId());
    }

    @Transactional
    public RegistrationDTO inscrever(
            Long teamId,
            ParticipantRegistrationRequest request,
            MultipartFile comprovante) {
        accessPolicyService.exigirEquipeDoParticipante(teamId);
        Robot robot = accessPolicyService.exigirRoboInscrevivelPeloParticipante(request.getRobotId());
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
        RegistrationDTO criada =
                registrationService.criarPorParticipante(dto, accessPolicyService.usuarioAtual());
        return registrationService.anexarComprovantePorParticipante(criada.getId(), comprovante);
    }

    @Transactional(readOnly = true)
    public RegistrationReceiptStorageService.ReceiptFile comprovanteInscricao(Long registrationId) {
        accessPolicyService.exigirInscricaoVisivelAoParticipante(registrationId);
        return registrationService.comprovanteDoParticipante(registrationId);
    }

    @Transactional
    public void cancelarInscricao(Long registrationId) {
        accessPolicyService.exigirInscricaoGerenciavelPeloParticipante(registrationId);
        registrationService.cancelarPorParticipante(registrationId);
    }

    @Transactional
    public RegistrationDTO reativarInscricao(Long registrationId) {
        accessPolicyService.exigirInscricaoGerenciavelPeloParticipante(registrationId);
        return registrationService.reativarPorParticipante(registrationId);
    }

    @Transactional
    public RegistrationCancellationRequestDTO solicitarCancelamento(Long registrationId, String motivo) {
        accessPolicyService.exigirInscricaoGerenciavelPeloParticipante(registrationId);
        return cancellationRequestService.solicitar(registrationId, accessPolicyService.usuarioAtual(), motivo);
    }

    @Transactional(readOnly = true)
    public List<RegistrationCancellationRequestDTO> solicitacoesCancelamento(Long registrationId) {
        accessPolicyService.exigirInscricaoGerenciavelPeloParticipante(registrationId);
        return cancellationRequestService.listarPorInscricao(registrationId);
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
