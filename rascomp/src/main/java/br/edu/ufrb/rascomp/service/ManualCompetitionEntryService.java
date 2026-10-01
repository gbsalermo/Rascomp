package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ManualCompetitionEntryRequest;
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
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final CompetitorService competitorService;
    private final RobotService robotService;
    private final RegistrationService registrationService;

    @Transactional
    public RegistrationDTO criar(ManualCompetitionEntryRequest request) {
        UserAccount dev = userAccountService.buscarAtual();
        if (dev.getRole() != UserRole.DEV) {
            throw new AccessDeniedException("A entrada manual de participante/robô é exclusiva do DEV.");
        }

        UserAccount participante = userAccountRepository.findById(request.getParticipantUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Conta PARTICIPANTE não encontrada: " + request.getParticipantUserId()));

        if (participante.getRole() != UserRole.PARTICIPANTE || !Boolean.TRUE.equals(participante.getAtivo())) {
            throw new IllegalArgumentException(
                    "Selecione uma conta PARTICIPANTE ativa criada pelo próprio participante.");
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + request.getTeamId()));

        if (!Boolean.TRUE.equals(team.getAtivo()) || !Boolean.TRUE.equals(team.getInstitution().getAtivo())) {
            throw new IllegalArgumentException("A equipe e sua instituição devem estar ativas.");
        }

        CompetitorDTO competitor = garantirCompetidor(participante, team);

        RobotDTO robotRequest = new RobotDTO();
        robotRequest.setNome(request.getRobotNome());
        robotRequest.setDescricao(request.getRobotDescricao());
        robotRequest.setTeamId(team.getId());
        robotRequest.setAtivo(true);
        RobotDTO robot = robotService.criar(robotRequest);

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

    private CompetitorDTO garantirCompetidor(UserAccount participante, Team team) {
        Competitor existente = competitorRepository.findByUserAccountId(participante.getId())
                .orElseGet(() -> competitorRepository.findByEmailIgnoreCase(participante.getEmail()).orElse(null));

        if (existente != null) {
            if (!existente.getTeam().getId().equals(team.getId())) {
                throw new IllegalArgumentException(
                        "A conta já está vinculada ao competidor " + existente.getNome()
                                + " da equipe " + existente.getTeam().getNome()
                                + ". Use essa equipe ou ajuste o vínculo antes da entrada manual.");
            }

            if (existente.getUserAccount() == null) {
                CompetitorDTO dto = new CompetitorDTO(existente);
                dto.setUserAccountId(participante.getId());
                dto.setNome(participante.getNome());
                dto.setEmail(participante.getEmail());
                dto.setTelefone(participante.getTelefone());
                dto.setTeamId(team.getId());
                dto.setAtivo(true);
                return competitorService.atualizar(existente.getId(), dto);
            }

            if (!Boolean.TRUE.equals(existente.getAtivo())) {
                throw new IllegalArgumentException(
                        "O competidor vinculado à conta está inativo. Reative a conta/competidor antes de continuar.");
            }

            return new CompetitorDTO(existente);
        }

        CompetitorDTO dto = new CompetitorDTO();
        dto.setNome(participante.getNome());
        dto.setEmail(participante.getEmail());
        dto.setTelefone(participante.getTelefone());
        dto.setTeamId(team.getId());
        dto.setUserAccountId(participante.getId());
        dto.setAtivo(true);
        return competitorService.criar(dto);
    }
}
