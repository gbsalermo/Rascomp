package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Competitor;

@Repository
public interface CompetitorRepository extends JpaRepository<Competitor, Long>{

	
	List<Competitor> findAllByOrderByNomeAsc();
	
	List<Competitor> findByAtivoTrueOrderByNomeAsc();

    List<Competitor> findByTeamIdOrderByNomeAsc(Long teamId);

    List<Competitor> findByTeamIdAndAtivoTrueOrderByNomeAsc(Long teamId);

    Optional<Competitor> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
