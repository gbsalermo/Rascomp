package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Bracket;

@Repository
public interface BracketRepository extends JpaRepository<Bracket, Long> {
    List<Bracket> findAllByOrderByDataCadastroDesc();
    List<Bracket> findByAtivoTrueOrderByDataCadastroDesc();
    List<Bracket> findByCompetitionIdOrderByDataCadastroDesc(Long competitionId);
    boolean existsByCompetitionIdAndCategoryId(Long competitionId, Long categoryId);
    boolean existsByCompetitionIdAndCategoryIdAndIdNot(Long competitionId, Long categoryId, Long id);
}
