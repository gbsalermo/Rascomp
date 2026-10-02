package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final UserAccountService userAccountService;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotResponsibleRepository robotResponsibleRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public UserAccount usuarioAtual() {
        return userAccountService.buscarAtual();
    }

    @Transactional(readOnly = true)
    public Team exigirEquipeDoParticipante(Long teamId) {
        UserAccount usuario = usuarioAtual();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + teamId));

        if (ehResponsavel(team, usuario)) {
            return team;
        }

        boolean membro = competitorRepository.findByUserAccountId(usuario.getId())
                .filter(competitor -> Boolean.TRUE.equals(competitor.getAtivo()))
                .map(Competitor::getTeam)
                .map(Team::getId)
                .filter(teamId::equals)
                .isPresent();

        if (!membro) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não pertence a esta equipe.");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public Team exigirEquipeDoResponsavel(Long teamId) {
        UserAccount usuario = usuarioAtual();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + teamId));

        if (!ehResponsavel(team, usuario)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não é o responsável por esta equipe.");
        }
        return team;
    }

    @Transactional(readOnly = true)
    public Competitor exigirCompetidorDaEquipe(Long competitorId) {
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> new EntityNotFoundException("Competidor não encontrado: " + competitorId));
        exigirEquipeDoResponsavel(competitor.getTeam().getId());
        return competitor;
    }

    @Transactional(readOnly = true)
    public Robot exigirRoboDaEquipe(Long robotId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + robotId));
        exigirEquipeDoResponsavel(robot.getTeam().getId());
        return robot;
    }

    @Transactional(readOnly = true)
    public Registration exigirInscricaoDaEquipe(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + registrationId));
        exigirEquipeDoResponsavel(registration.getTeam().getId());
        return registration;
    }

    @Transactional(readOnly = true)
    public Registration exigirInscricaoVisivelAoParticipante(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + registrationId));
        UserAccount usuario = usuarioAtual();

        if (ehResponsavel(registration.getTeam(), usuario)
                || usuarioEhResponsavelPeloRobo(registration.getRobot().getId(), usuario.getId())) {
            return registration;
        }

        boolean participa = registration.getCompetitors().stream()
                .map(Competitor::getUserAccount)
                .filter(java.util.Objects::nonNull)
                .anyMatch(conta -> conta.getId().equals(usuario.getId()));

        if (!participa) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não participa desta inscrição nem é responsável pelo robô.");
        }
        return registration;
    }

    @Transactional(readOnly = true)
    public Registration exigirInscricaoGerenciavelPeloParticipante(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + registrationId));
        UserAccount usuario = usuarioAtual();

        if (ehResponsavel(registration.getTeam(), usuario)
                || (registration.getRobot().getCreatedByUser() != null
                    && registration.getRobot().getCreatedByUser().getId().equals(usuario.getId()))) {
            return registration;
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "Somente o líder da equipe ou quem cadastrou o robô pode administrar esta inscrição.");
    }

    @Transactional(readOnly = true)
    public Robot exigirRoboVisivelAoParticipante(Long robotId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + robotId));
        UserAccount usuario = usuarioAtual();

        if (ehResponsavel(robot.getTeam(), usuario)) {
            return robot;
        }

        boolean responsavelPeloRobo = usuarioEhResponsavelPeloRobo(robotId, usuario.getId());

        if (!responsavelPeloRobo) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não é responsável por este robô.");
        }
        return robot;
    }

    @Transactional(readOnly = true)
    public Robot exigirRoboGerenciavelPeloParticipante(Long robotId) {
        return exigirRoboVisivelAoParticipante(robotId);
    }

    @Transactional(readOnly = true)
    public Robot exigirRoboInscrevivelPeloParticipante(Long robotId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + robotId));
        UserAccount usuario = usuarioAtual();

        if (ehResponsavel(robot.getTeam(), usuario)) {
            return robot;
        }

        if (robot.getCreatedByUser() != null
                && robot.getCreatedByUser().getId().equals(usuario.getId())) {
            return robot;
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "Somente quem cadastrou o robô ou o líder da equipe pode iniciar a inscrição dele.");
    }

    public boolean ehResponsavel(Team team, UserAccount usuario) {
        return team.getResponsibleUser() != null
                && team.getResponsibleUser().getId().equals(usuario.getId());
    }

    private boolean usuarioEhResponsavelPeloRobo(Long robotId, Long userAccountId) {
        return competitorRepository.findByUserAccountId(userAccountId)
                .filter(competitor -> Boolean.TRUE.equals(competitor.getAtivo()))
                .map(competitor -> robotResponsibleRepository
                        .existsByRobotIdAndCompetitorIdAndAtivoTrue(robotId, competitor.getId()))
                .orElse(false);
    }
}
