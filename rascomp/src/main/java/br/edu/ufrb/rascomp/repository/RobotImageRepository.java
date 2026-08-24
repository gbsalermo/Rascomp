package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RobotImage;

@Repository
public interface RobotImageRepository extends JpaRepository<RobotImage, Long> {
    List<RobotImage> findByRobotIdAndAtivoTrueOrderByPrincipalDescOrdemAscIdAsc(Long robotId);
    Optional<RobotImage> findByIdAndRobotId(Long id, Long robotId);
    Optional<RobotImage> findFirstByRobotIdAndPrincipalTrueAndAtivoTrue(Long robotId);
}
