package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

	
	Optional<Institution> findBySiglaIgnoreCase(String sigla);
	
	boolean existsBySiglaIgnoreCase(String sigla);
	
	boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, Long id);
	
	List<Institution> findByAtivoTrueOrderByNomeAsc();
	
	List<Institution> findAllByOrderByNomeAsc();
}
