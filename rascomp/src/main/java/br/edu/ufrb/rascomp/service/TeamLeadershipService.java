package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.TeamLeaderTransferRequest;
import br.edu.ufrb.rascomp.dto.TeamLeadershipHistoryDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TeamLeadershipHistory;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.TeamLeadershipHistoryRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamLeadershipService {

    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final CompetitionRepository competitionRepository;
    private final ParticipantCompetitionRegistrationRepository participantRegistrationRepository;
    private final TeamLeadershipHistoryRepository historyRepository;
    private final UserAccountService userAccountService;

    @Transactional(readOnly = true)
    public List<CompetitorDTO> candidatos(Long teamId, Long competitionId) {
        Team team = buscarTeam(teamId);
        buscarCompetition(competitionId);

        return competitorRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId)
                .stream()
                .filter(competitor -> competitor.getUserAccount() != null)
                .filter(competitor -> Boolean.TRUE.equals(competitor.getUserAccount().getAtivo()))
                .filter(competitor -> possuiInscricaoValida(
                        competitionId,
                        competitor.getId()))
                .map(CompetitorDTO::new)
                .toList();
    }

    @Transactional
    public TeamLeadershipHistoryDTO transferir(
            Long teamId,
            TeamLeaderTransferRequest request) {

        UserAccount dev = userAccountService.buscarAtual();
        if (dev.getRole() != UserRole.DEV) {
            throw new AccessDeniedException(
                    "A troca administrativa de líder é exclusiva do DEV.");
        }

        String motivo = normalizarMotivo(request.getMotivo());
        Team team = buscarTeam(teamId);
        Competition competition = buscarCompetition(request.getCompetitionId());

        Competitor novoLider = competitorRepository
                .findByUserAccountId(request.getNewResponsibleUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "O novo líder precisa possuir Competitor vinculado à própria conta."));

        if (!novoLider.getTeam().getId().equals(teamId)
                || !Boolean.TRUE.equals(novoLider.getAtivo())
                || novoLider.getUserAccount() == null
                || !Boolean.TRUE.equals(novoLider.getUserAccount().getAtivo())) {
            throw new IllegalArgumentException(
                    "O novo líder deve ser participante ativo e associado à mesma equipe.");
        }

        ParticipantCompetitionRegistration pessoal = participantRegistrationRepository
                .findByCompetitionIdAndCompetitorId(
                        competition.getId(),
                        novoLider.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "O novo líder precisa iniciar sua inscrição individual nesta competição."));

        if (pessoal.getStatus() != ParticipantCompetitionRegistrationStatus.PENDENTE
                && pessoal.getStatus() != ParticipantCompetitionRegistrationStatus.APROVADA) {
            throw new IllegalArgumentException(
                    "O novo líder deve possuir inscrição individual PENDENTE ou APROVADA.");
        }

        UserAccount anterior = team.getResponsibleUser();
        if (anterior != null && anterior.getId().equals(novoLider.getUserAccount().getId())) {
            throw new IllegalArgumentException("O participante selecionado já é o líder da equipe.");
        }

        team.setResponsibleUser(novoLider.getUserAccount());
        teamRepository.save(team);

        TeamLeadershipHistory history = new TeamLeadershipHistory();
        history.setTeam(team);
        history.setCompetition(competition);
        history.setPreviousUser(anterior);
        history.setNewUser(novoLider.getUserAccount());
        history.setChangedByUser(dev);
        history.setReason(motivo);

        return new TeamLeadershipHistoryDTO(historyRepository.save(history));
    }

    @Transactional(readOnly = true)
    public List<TeamLeadershipHistoryDTO> historico(Long teamId) {
        buscarTeam(teamId);
        return historyRepository.findByTeamIdOrderByDataCadastroDesc(teamId)
                .stream()
                .map(TeamLeadershipHistoryDTO::new)
                .toList();
    }

    private boolean possuiInscricaoValida(Long competitionId, Long competitorId) {
        return participantRegistrationRepository
                .findByCompetitionIdAndCompetitorId(competitionId, competitorId)
                .map(ParticipantCompetitionRegistration::getStatus)
                .map(status -> status == ParticipantCompetitionRegistrationStatus.PENDENTE
                        || status == ParticipantCompetitionRegistrationStatus.APROVADA)
                .orElse(false);
    }

    private Team buscarTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Equipe não encontrada: " + id));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competição não encontrada: " + id));
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe a justificativa da troca de liderança.");
        }
        String normalized = motivo.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("A justificativa deve possuir no máximo 500 caracteres.");
        }
        return normalized;
    }
}
