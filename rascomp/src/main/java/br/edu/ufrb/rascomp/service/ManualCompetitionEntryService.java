package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ManualCompetitionEntryRequest;
import br.edu.ufrb.rascomp.dto.ManualParticipantEntryRequest;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationDTO;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Entrada administrativa excepcional de participante/robô após o fluxo público.
 *
 * Fluxo:
 * conta PARTICIPANTE existente -> competidor/equipe -> robô -> inscrição APROVADA.
 *
 * A ação é exclusiva de DEV e auditada no histórico da inscrição. Para Sumô,
 * a inspeção continua obrigatória antes de uma nova geração de chave.
 */
@Service
@RequiredArgsConstructor
public class ManualCompetitionEntryService {

    private final UserAccountService userAccountService;
    private final UserAccountRepository userAccountRepository;
    private final CompetitorRepository competitorRepository;
    private final TeamRepository teamRepository;
    private final CompetitorService competitorService;
    private final ParticipantCompetitionRegistrationService participantCompetitionRegistrationService;
    private final RobotService robotService;
    private final RobotResponsibleService robotResponsibleService;
    private final RegistrationService registrationService;

    @Transactional
    public ParticipantCompetitionRegistrationDTO criarParticipante(
            ManualParticipantEntryRequest request) {

        UserAccount dev = userAccountService.buscarAtual();
        if (dev.getRole() != UserRole.DEV) {
            throw new AccessDeniedException(
                    "A entrada manual de participante é exclusiva do DEV.");
        }

        UserAccount participante = buscarParticipante(request.getParticipantUserId());
        Competitor competitor = resolverCompetidor(participante, request.getTeamId());

        return participantCompetitionRegistrationService.criarEntradaManualDev(
                request.getCompetitionId(),
                competitor,
                dev,
                request.getJustificativa());
    }

    @Transactional
    public RegistrationDTO criar(ManualCompetitionEntryRequest request) {
        UserAccount dev = userAccountService.buscarAtual();
        if (dev.getRole() != UserRole.DEV) {
            throw new AccessDeniedException("A entrada manual de participante/robô é exclusiva do DEV.");
        }

        UserAccount participante = buscarParticipante(request.getParticipantUserId());

        Competitor competitor = resolverCompetidor(participante, request.getTeamId());
        Team team = competitor.getTeam();

        participantCompetitionRegistrationService.criarEntradaManualDev(
                request.getCompetitionId(),
                competitor,
                dev,
                request.getJustificativa());

        RobotDTO robotRequest = new RobotDTO();
        robotRequest.setNome(request.getRobotNome());
        robotRequest.setDescricao(request.getRobotDescricao());
        robotRequest.setTeamId(team.getId());
        robotRequest.setAtivo(true);
        RobotDTO robot = robotService.criar(robotRequest, participante);
        robotResponsibleService.associarManual(robot.getId(), competitor.getId(), dev);

        RegistrationDTO registration = new RegistrationDTO();
        registration.setCompetitionId(request.getCompetitionId());
        registration.setCategoryId(request.getCategoryId());
        registration.setTeamId(team.getId());
        registration.setRobotId(robot.getId());
        registration.setCompetitorIds(List.of(competitor.getId()));
        registration.setObservacao(
                "Entrada manual DEV. Participante: " + participante.getNome() + " <" + participante.getEmail() + ">.");

        return registrationService.criarEntradaManualDev(
                registration,
                dev,
                request.getJustificativa());
    }

    private UserAccount buscarParticipante(Long userId) {
        UserAccount participante = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Conta PARTICIPANTE não encontrada: " + userId));

        if (participante.getRole() != UserRole.PARTICIPANTE
                || !Boolean.TRUE.equals(participante.getAtivo())) {
            throw new IllegalArgumentException(
                    "Selecione uma conta PARTICIPANTE ativa criada pelo próprio participante.");
        }
        return participante;
    }

    private Competitor resolverCompetidor(UserAccount participante, Long teamId) {
        Competitor existente = competitorRepository.findByUserAccountId(participante.getId())
                .orElse(null);

        if (existente != null) {
            if (teamId != null && !existente.getTeam().getId().equals(teamId)) {
                throw new IllegalArgumentException(
                        "O participante já está associado à equipe "
                                + existente.getTeam().getNome()
                                + ". A entrada manual não transfere participante entre equipes.");
            }
            validarVinculoAtivo(existente);
            return existente;
        }

        if (teamId == null) {
            throw new IllegalArgumentException(
                    "Selecione a equipe para vincular a nova conta PARTICIPANTE.");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Equipe não encontrada: " + teamId));

        if (!Boolean.TRUE.equals(team.getAtivo())
                || !Boolean.TRUE.equals(team.getInstitution().getAtivo())) {
            throw new IllegalArgumentException(
                    "A equipe e a instituição precisam estar ativas.");
        }

        CompetitorDTO dto = new CompetitorDTO();
        dto.setNome(participante.getNome());
        dto.setEmail(participante.getEmail());
        dto.setTelefone(participante.getTelefone());
        dto.setTeamId(team.getId());
        dto.setUserAccountId(participante.getId());
        dto.setAtivo(true);
        competitorService.criar(dto);

        Competitor criado = competitorRepository.findByUserAccountId(participante.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi possível criar o vínculo competitivo do participante."));
        validarVinculoAtivo(criado);
        return criado;
    }

    private void validarVinculoAtivo(Competitor competitor) {
        Team team = competitor.getTeam();
        if (!Boolean.TRUE.equals(competitor.getAtivo())
                || !Boolean.TRUE.equals(team.getAtivo())
                || !Boolean.TRUE.equals(team.getInstitution().getAtivo())) {
            throw new IllegalArgumentException(
                    "O competidor, a equipe e a instituição precisam estar ativos.");
        }
    }

}
