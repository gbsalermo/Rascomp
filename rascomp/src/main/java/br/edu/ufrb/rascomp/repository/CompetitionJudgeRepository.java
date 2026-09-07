package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.CompetitionJudge;

@Repository
public interface CompetitionJudgeRepository extends JpaRepository<CompetitionJudge, Long> {

    List<CompetitionJudge> findByCompetitionIdOrderByNomeAsc(Long competitionId);

    List<CompetitionJudge> findByCompetitionIdAndAtivoTrueOrderByNomeAsc(Long competitionId);
}
