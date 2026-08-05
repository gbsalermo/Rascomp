package br.edu.ufrb.rascomp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.ConfigSumo;

@Repository
public interface ConfigSumoRepository extends JpaRepository<ConfigSumo, Long>{
	
	Optional<ConfigSumo> findByCategoryId(Long competitionCategoryId);
	
	boolean existsByCompetitionCategoryId(Long competitionCategoryId);

}
