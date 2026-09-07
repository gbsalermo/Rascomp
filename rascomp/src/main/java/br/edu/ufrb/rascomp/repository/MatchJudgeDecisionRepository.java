package br.edu.ufrb.rascomp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.MatchJudgeDecision;

@Repository
public interface MatchJudgeDecisionRepository extends JpaRepository<MatchJudgeDecision, Long> {

    boolean existsByMatchId(Long matchId);

    Optional<MatchJudgeDecision> findByMatchId(Long matchId);
}
