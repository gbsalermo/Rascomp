package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.FollowTakeScheduleEntry;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow;

@Repository
public interface FollowTakeScheduleEntryRepository extends JpaRepository<FollowTakeScheduleEntry, Long> {

    List<FollowTakeScheduleEntry> findByScheduleIdOrderByOrdemConvocacaoAsc(Long scheduleId);

    Optional<FollowTakeScheduleEntry> findByScheduleIdAndRegistrationId(Long scheduleId, Long registrationId);

    Optional<FollowTakeScheduleEntry>
            findByScheduleCompetitionIdAndScheduleCategoryIdAndScheduleTomadaAndRegistrationId(
                    Long competitionId,
                    Long categoryId,
                    Integer tomada,
                    Long registrationId);

    long countByScheduleIdAndStatus(Long scheduleId, StatusConvocacaoFollow status);
}
