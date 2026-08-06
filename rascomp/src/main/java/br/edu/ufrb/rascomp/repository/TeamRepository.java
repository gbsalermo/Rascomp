package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
	
	List<Team> findAllByOrderByNomeAsc();
	
	List<Team> findByAtivoTrueOrderByNomeAsc();
	
	List<Team> findByInstitutionIdOrderByNomeAsc(Long institutionId);

	List<Team> findByInstitutionIdAndAtivoTrueOrderByNomeAsc(Long institutionId);

    boolean existsByNomeIgnoreCaseAndInstitutionId(String nome, Long institutionId);

    boolean existsByNomeIgnoreCaseAndInstitutionIdAndIdNot(String nome, Long institutionId,Long id);
}
