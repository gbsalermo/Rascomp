package br.edu.ufrb.rascomp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.ConfigFollow;

@Repository
public interface ConfigFollowRepository extends JpaRepository<ConfigFollow, Long>{
	
	Optional<ConfigFollow> findByCompetitionCategoryId(Long competitionCategoryId);

    boolean existsByCompetitionCategoryId(Long competitionCategoryId);

}
