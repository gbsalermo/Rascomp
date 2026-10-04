package br.edu.ufrb.rascomp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.FollowManualResult;

@Repository
public interface FollowManualResultRepository extends JpaRepository<FollowManualResult, Long> {

    Optional<FollowManualResult> findByCompetitionIdAndCategoryId(Long competitionId, Long categoryId);

    boolean existsByCompetitionIdAndCategoryId(Long competitionId, Long categoryId);
}
