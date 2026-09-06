package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.CompetitionRegistrationWindowChange;

@Repository
public interface CompetitionRegistrationWindowChangeRepository extends JpaRepository<CompetitionRegistrationWindowChange, Long> {
    List<CompetitionRegistrationWindowChange> findByCompetitionIdOrderByDataCadastroDesc(Long competitionId);
}
