package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Robot;

@Repository
public interface RobotRepository extends JpaRepository<Robot, Long>{
	
	
	List<Robot> findAllByOrderByNomeAsc();

    List<Robot> findByAtivoTrueOrderByNomeAsc();

    List<Robot> findByTeamIdOrderByNomeAsc(Long teamId);

    List<Robot> findByTeamIdAndAtivoTrueOrderByNomeAsc(Long teamId);

    boolean existsByNomeIgnoreCaseAndTeamId(String nome, Long teamId);

    boolean existsByNomeIgnoreCaseAndTeamIdAndIdNot(String nome, Long teamId, Long id);
	

}
