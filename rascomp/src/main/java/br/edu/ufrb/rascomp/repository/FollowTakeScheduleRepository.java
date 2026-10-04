package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.FollowTakeSchedule;

@Repository
public interface FollowTakeScheduleRepository extends JpaRepository<FollowTakeSchedule, Long> {

    List<FollowTakeSchedule> findByCompetitionIdAndAtivoTrueOrderByDataHoraAscOrdemExecucaoAsc(Long competitionId);

    List<FollowTakeSchedule> findByCompetitionIdAndCategoryIdAndAtivoTrueOrderByTomadaAsc(
            Long competitionId,
            Long categoryId);

    Optional<FollowTakeSchedule> findByCompetitionIdAndCategoryIdAndTomada(
            Long competitionId,
            Long categoryId,
            Integer tomada);

    boolean existsByCompetitionIdAndCategoryIdAndTomada(
            Long competitionId,
            Long categoryId,
            Integer tomada);
}
