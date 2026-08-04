package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;

@Repository
public interface CompetitionCategoryRepository extends JpaRepository<CompetitionCategory, Long> {

	 List<CompetitionCategory> findByModalidade(Modalidade modalidade);

	 List<CompetitionCategory> findByModalidadeAndAtivoTrue(Modalidade modalidade);
}
