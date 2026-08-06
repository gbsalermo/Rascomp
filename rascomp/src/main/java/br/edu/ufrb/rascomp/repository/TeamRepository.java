package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
	
	List<Team> findAllByOrderByNomeAsc();
	
	List<Team> findByAtivoTrueOrderByNomeAsc();

	List<Team> findByInstitutionId();
	
	List<Team> findByAtivoTrueAndInstitutionId();
}
