package br.edu.ufrb.rascomp.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.dto.RobotResponsibleDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RobotResponsibleService {

    private final AccessPolicyService accessPolicyService;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotResponsibleRepository responsibleRepository;

    @Transactional(readOnly = true)
    public List<RobotResponsibleDTO> listar(Long robotId) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + robotId));
        accessPolicyService.exigirEquipeDoParticipante(robot.getTeam().getId());

        return responsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(robotId)
                .stream()
                .map(RobotResponsibleDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RobotDTO> listarRobosDoCompetidorAtual(Long teamId) {
        UserAccount usuario = accessPolicyService.usuarioAtual();
        Competitor competitor = competitorRepository.findByUserAccountId(usuario.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Sua conta ainda não possui vínculo competitivo com uma equipe."));

        if (!competitor.getTeam().getId().equals(teamId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não pertence a esta equipe.");
        }

        return responsibleRepository
                .findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(competitor.getId())
                .stream()
                .map(RobotResponsible::getRobot)
                .filter(robot -> Boolean.TRUE.equals(robot.getAtivo()))
                .map(RobotDTO::new)
                .toList();
    }

    @Transactional
    public List<RobotResponsibleDTO> definir(Long robotId, Set<Long> competitorIds) {
        Robot robot = accessPolicyService.exigirRoboDaEquipe(robotId);
        UserAccount lider = accessPolicyService.usuarioAtual();

        Set<Long> ids = competitorIds == null
                ? Set.of()
                : new LinkedHashSet<>(competitorIds);

        List<Competitor> competitors = ids.stream()
                .map(id -> competitorRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Competidor não encontrado: " + id)))
                .toList();

        competitors.forEach(competitor -> {
            if (!competitor.getTeam().getId().equals(robot.getTeam().getId())
                    || !Boolean.TRUE.equals(competitor.getAtivo())) {
                throw new IllegalArgumentException(
                        "Todos os responsáveis devem ser competidores ativos da mesma equipe do robô.");
            }
        });

        List<RobotResponsible> atuais =
                responsibleRepository.findByRobotIdOrderByCompetitorNomeAsc(robotId);

        atuais.forEach(link -> {
            link.setAtivo(ids.contains(link.getCompetitor().getId()));
            responsibleRepository.save(link);
        });

        for (Competitor competitor : competitors) {
            RobotResponsible link = responsibleRepository
                    .findByRobotIdAndCompetitorId(robotId, competitor.getId())
                    .orElseGet(RobotResponsible::new);
            link.setRobot(robot);
            link.setCompetitor(competitor);
            link.setCreatedByUser(lider);
            link.setAtivo(true);
            responsibleRepository.save(link);
        }

        return listar(robotId);
    }

    @Transactional
    public void associarCriador(Long robotId, UserAccount usuario) {
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + robotId));

        Competitor competitor = competitorRepository.findByUserAccountId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "A conta precisa estar associada como competidor da equipe antes de cadastrar robô."));

        if (!competitor.getTeam().getId().equals(robot.getTeam().getId())) {
            throw new IllegalArgumentException(
                    "O competidor responsável deve pertencer à mesma equipe do robô.");
        }

        RobotResponsible link = responsibleRepository
                .findByRobotIdAndCompetitorId(robotId, competitor.getId())
                .orElseGet(RobotResponsible::new);

        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setCreatedByUser(usuario);
        link.setAtivo(true);
        responsibleRepository.save(link);
    }

    @Transactional(readOnly = true)
    public boolean usuarioEhResponsavel(Long robotId, Long userAccountId) {
        return competitorRepository.findByUserAccountId(userAccountId)
                .map(competitor -> responsibleRepository.existsByRobotIdAndCompetitorIdAndAtivoTrue(
                        robotId,
                        competitor.getId()))
                .orElse(false);
    }
}
