package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RobotResponsible;

@Repository
public interface RobotResponsibleRepository extends JpaRepository<RobotResponsible, Long> {

    List<RobotResponsible> findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(Long robotId);

    List<RobotResponsible> findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(Long competitorId);

    Optional<RobotResponsible> findByRobotIdAndCompetitorId(Long robotId, Long competitorId);

    boolean existsByRobotIdAndCompetitorIdAndAtivoTrue(Long robotId, Long competitorId);
}
