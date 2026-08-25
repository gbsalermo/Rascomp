package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import jakarta.persistence.LockModeType;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    List<Competition> findAllByOrderByDataInicioDesc();
    List<Competition> findByAtivoTrueOrderByDataInicioDesc();
    List<Competition> findByStatusOrderByDataInicioDesc(StatusCompetition status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Competition c where c.id = :id")
    Optional<Competition> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
