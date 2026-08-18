package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RoundSumo;

@Repository
public interface RoundSumoRepository extends JpaRepository<RoundSumo, Long> {

    List<RoundSumo> findByMatchIdOrderByNumeroRoundAsc(Long matchId);

    long countByMatchId(Long matchId);

    boolean existsByMatchIdAndNumeroRound(Long matchId, Integer numeroRound);
}
