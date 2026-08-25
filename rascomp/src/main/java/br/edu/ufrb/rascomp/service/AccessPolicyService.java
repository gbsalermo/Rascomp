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
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public UserAccount usuarioAtual() {
        return userAccountService.buscarAtual();
    }

    @Transactional(readOnly = true)
    public Team exigirEquipeDoResponsavel(Long teamId) {
        UserAccount usuario = usuarioAtual();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + teamId));

        if (team.getResponsibleUser() == null
                || !team.getResponsibleUser().getId().equals(usuario.getId())) {
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
}
