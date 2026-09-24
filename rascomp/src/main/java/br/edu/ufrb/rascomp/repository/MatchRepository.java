package br.edu.ufrb.rascomp.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByBracketIdOrderByRodadaAscOrdemAsc(Long bracketId);
    Optional<Match> findByBracketIdAndRodadaAndOrdem(Long bracketId, Integer rodada, Integer ordem);
    boolean existsByBracketIdAndRodadaAndOrdem(Long bracketId, Integer rodada, Integer ordem);
    boolean existsByBracketIdAndRodadaAndOrdemAndIdNot(Long bracketId, Integer rodada, Integer ordem, Long id);
    boolean existsByRegistrationAIdOrRegistrationBId(Long registrationAId, Long registrationBId);
    boolean existsByBracketCompetitionIdAndStatusIn(Long competitionId, Collection<StatusMatch> statuses);

    @Query("""
            select m
            from Match m
            join fetch m.bracket b
            join fetch b.competition c
            join fetch b.category cat
            where c.id = :competitionId
              and b.atual = true
              and b.ativo = true
              and m.ativo = true
            order by m.dataHora asc, m.ordemExecucao asc, m.id asc
            """)
    List<Match> findAgendaAtualByCompetitionId(@Param("competitionId") Long competitionId);
}
