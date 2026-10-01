package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;

@Repository
public interface ParticipantCompetitionRegistrationRepository
        extends JpaRepository<ParticipantCompetitionRegistration, Long> {

    Optional<ParticipantCompetitionRegistration> findByCompetitionIdAndCompetitorId(
            Long competitionId,
            Long competitorId);

    boolean existsByCompetitionIdAndCompetitorId(Long competitionId, Long competitorId);

    boolean existsByCompetitionIdAndCompetitorIdAndStatusAndAtivoTrue(
            Long competitionId,
            Long competitorId,
            ParticipantCompetitionRegistrationStatus status);

    List<ParticipantCompetitionRegistration> findByCompetitorIdOrderByDataCadastroDesc(Long competitorId);

    List<ParticipantCompetitionRegistration> findByCompetitionIdOrderByDataCadastroDesc(Long competitionId);
}
