package br.edu.ufrb.rascomp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.MatchResult;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    Optional<MatchResult> findByMatchId(Long matchId);
    boolean existsByMatchId(Long matchId);
    boolean existsByMatchIdAndIdNot(Long matchId, Long id);
}
