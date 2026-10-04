package br.edu.ufrb.rascomp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.TeamInviteCreateRequest;
import br.edu.ufrb.rascomp.dto.TeamJoinCreateRequest;
import br.edu.ufrb.rascomp.dto.TeamMembershipRequestDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TeamMembershipRequest;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipRequestType;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipStatus;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.TeamMembershipRequestRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamMembershipService {

    private final AccessPolicyService accessPolicyService;
    private final UserAccountRepository userAccountRepository;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final TeamMembershipRequestRepository requestRepository;

    @Transactional
    public TeamMembershipRequestDTO convidar(Long teamId, TeamInviteCreateRequest request) {
        Team team = accessPolicyService.exigirEquipeDoResponsavel(teamId);
        UserAccount lider = accessPolicyService.usuarioAtual();

        UserAccount participante = userAccountRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhuma conta PARTICIPANTE encontrada para o e-mail informado."));

        validarContaParticipante(participante);
        validarSemEquipe(participante);
        validarSemSolicitacaoPendente(team.getId(), participante.getId());

        TeamMembershipRequest entity = new TeamMembershipRequest();
        entity.setTeam(team);
        entity.setParticipantUser(participante);
        entity.setRequestedByUser(lider);
        entity.setRequestType(TeamMembershipRequestType.CONVITE);
        entity.setStatus(TeamMembershipStatus.PENDENTE);
        entity.setMensagem(normalizar(request.getMensagem()));

        return new TeamMembershipRequestDTO(requestRepository.save(entity));
    }

    @Transactional
    public TeamMembershipRequestDTO solicitarEntrada(Long teamId, TeamJoinCreateRequest request) {
        UserAccount participante = accessPolicyService.usuarioAtual();
        validarContaParticipante(participante);
        validarSemEquipe(participante);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + teamId));

        if (!Boolean.TRUE.equals(team.getAtivo())) {
            throw new IllegalArgumentException("A equipe selecionada está inativa.");
        }

        validarSemSolicitacaoPendente(team.getId(), participante.getId());

        TeamMembershipRequest entity = new TeamMembershipRequest();
        entity.setTeam(team);
        entity.setParticipantUser(participante);
        entity.setRequestedByUser(participante);
        entity.setRequestType(TeamMembershipRequestType.SOLICITACAO);
        entity.setStatus(TeamMembershipStatus.PENDENTE);
        entity.setMensagem(normalizar(request.getMensagem()));

        return new TeamMembershipRequestDTO(requestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TeamMembershipRequestDTO> minhasSolicitacoes() {
        UserAccount usuario = accessPolicyService.usuarioAtual();
        return requestRepository.findByParticipantUserIdOrderByDataCadastroDesc(usuario.getId())
                .stream()
                .map(TeamMembershipRequestDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMembershipRequestDTO> solicitacoesDaEquipe(Long teamId) {
        accessPolicyService.exigirEquipeDoResponsavel(teamId);
        return requestRepository.findByTeamIdOrderByDataCadastroDesc(teamId)
                .stream()
                .map(TeamMembershipRequestDTO::new)
                .toList();
    }

    @Transactional
    public TeamMembershipRequestDTO aceitarConvite(Long requestId) {
        TeamMembershipRequest request = buscar(requestId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        validarPendente(request);
        if (request.getRequestType() != TeamMembershipRequestType.CONVITE) {
            throw new IllegalArgumentException("Esta solicitação não é um convite.");
        }
        if (!request.getParticipantUser().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Este convite pertence a outro participante.");
        }

        vincularCompetidor(request.getParticipantUser(), request.getTeam());
        concluir(request, TeamMembershipStatus.ACEITA, usuario);
        return new TeamMembershipRequestDTO(request);
    }

    @Transactional
    public TeamMembershipRequestDTO rejeitarConvite(Long requestId) {
        TeamMembershipRequest request = buscar(requestId);
        UserAccount usuario = accessPolicyService.usuarioAtual();

        validarPendente(request);
        if (request.getRequestType() != TeamMembershipRequestType.CONVITE
                || !request.getParticipantUser().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Este convite não pode ser recusado por esta conta.");
        }

        concluir(request, TeamMembershipStatus.REJEITADA, usuario);
        return new TeamMembershipRequestDTO(request);
    }

    @Transactional
    public TeamMembershipRequestDTO aprovarSolicitacao(Long requestId) {
        TeamMembershipRequest request = buscar(requestId);
        UserAccount lider = accessPolicyService.usuarioAtual();

        validarPendente(request);
        if (request.getRequestType() != TeamMembershipRequestType.SOLICITACAO) {
            throw new IllegalArgumentException("Este registro não é uma solicitação de entrada.");
        }
        exigirLider(request.getTeam(), lider);

        vincularCompetidor(request.getParticipantUser(), request.getTeam());
        concluir(request, TeamMembershipStatus.ACEITA, lider);
        return new TeamMembershipRequestDTO(request);
    }

    @Transactional
    public TeamMembershipRequestDTO rejeitarSolicitacao(Long requestId) {
        TeamMembershipRequest request = buscar(requestId);
        UserAccount lider = accessPolicyService.usuarioAtual();

        validarPendente(request);
        if (request.getRequestType() != TeamMembershipRequestType.SOLICITACAO) {
            throw new IllegalArgumentException("Este registro não é uma solicitação de entrada.");
        }
        exigirLider(request.getTeam(), lider);

        concluir(request, TeamMembershipStatus.REJEITADA, lider);
        return new TeamMembershipRequestDTO(request);
    }

    private void vincularCompetidor(UserAccount participante, Team team) {
        validarContaParticipante(participante);

        Competitor porConta = competitorRepository.findByUserAccountId(participante.getId()).orElse(null);
        if (porConta != null) {
            if (porConta.getTeam().getId().equals(team.getId()) && Boolean.TRUE.equals(porConta.getAtivo())) {
                return;
            }
            throw new IllegalArgumentException(
                    "A conta já está vinculada à equipe " + porConta.getTeam().getNome() + ".");
        }

        Competitor porEmail = competitorRepository.findByEmailIgnoreCase(participante.getEmail()).orElse(null);
        if (porEmail != null) {
            if (!porEmail.getTeam().getId().equals(team.getId())) {
                throw new IllegalArgumentException(
                        "Já existe um competidor com este e-mail em outra equipe.");
            }
            if (porEmail.getUserAccount() != null
                    && !porEmail.getUserAccount().getId().equals(participante.getId())) {
                throw new IllegalArgumentException("O competidor já está associado a outra conta.");
            }

            porEmail.setUserAccount(participante);
            porEmail.setNome(participante.getNome());
            porEmail.setTelefone(participante.getTelefone());
            porEmail.setAtivo(true);
            competitorRepository.save(porEmail);
            return;
        }

        Competitor competitor = new Competitor();
        competitor.setNome(participante.getNome());
        competitor.setEmail(participante.getEmail());
        competitor.setTelefone(participante.getTelefone());
        competitor.setTeam(team);
        competitor.setUserAccount(participante);
        competitor.setAtivo(true);
        competitorRepository.save(competitor);
    }

    private void validarContaParticipante(UserAccount participante) {
        if (participante.getRole() != UserRole.PARTICIPANTE || !Boolean.TRUE.equals(participante.getAtivo())) {
            throw new IllegalArgumentException("A conta precisa ser uma conta PARTICIPANTE ativa.");
        }
    }

    private void validarSemEquipe(UserAccount participante) {
        competitorRepository.findByUserAccountId(participante.getId()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "A conta já está associada à equipe " + existing.getTeam().getNome() + ".");
        });
    }

    private void validarSemSolicitacaoPendente(Long teamId, Long participantUserId) {
        if (requestRepository.existsByTeamIdAndParticipantUserIdAndStatus(
                teamId, participantUserId, TeamMembershipStatus.PENDENTE)) {
            throw new IllegalArgumentException(
                    "Já existe um convite ou solicitação pendente entre esta conta e a equipe.");
        }
    }

    private TeamMembershipRequest buscar(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Convite/solicitação de equipe não encontrado: " + id));
    }

    private void validarPendente(TeamMembershipRequest request) {
        if (request.getStatus() != TeamMembershipStatus.PENDENTE) {
            throw new IllegalArgumentException("Este convite/solicitação já foi processado.");
        }
    }

    private void exigirLider(Team team, UserAccount usuario) {
        if (team.getResponsibleUser() == null
                || !team.getResponsibleUser().getId().equals(usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o líder da equipe pode analisar esta solicitação.");
        }
    }

    private void concluir(
            TeamMembershipRequest request,
            TeamMembershipStatus status,
            UserAccount reviewedBy) {
        request.setStatus(status);
        request.setReviewedByUser(reviewedBy);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private String normalizar(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
