package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Match;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByBracketIdOrderByRodadaAscOrdemAsc(Long bracketId);
    Optional<Match> findByBracketIdAndRodadaAndOrdem(Long bracketId, Integer rodada, Integer ordem);
    boolean existsByBracketIdAndRodadaAndOrdem(Long bracketId, Integer rodada, Integer ordem);
    boolean existsByBracketIdAndRodadaAndOrdemAndIdNot(Long bracketId, Integer rodada, Integer ordem, Long id);
}
