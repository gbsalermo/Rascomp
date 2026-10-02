package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RegistrationCompetitorChange;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;

@Repository
public interface RegistrationCompetitorChangeRepository
        extends JpaRepository<RegistrationCompetitorChange, Long> {

    List<RegistrationCompetitorChange> findByRegistrationIdOrderByDataCadastroDesc(Long registrationId);

    List<RegistrationCompetitorChange> findByRegistrationCompetitionIdAndStatusOrderByDataCadastroDesc(
            Long competitionId,
            RegistrationCompetitorChangeStatus status);

    Optional<RegistrationCompetitorChange>
            findTopByRegistrationIdAndCompetitorIdAndChangeTypeOrderByIdDesc(
                    Long registrationId,
                    Long competitorId,
                    RegistrationCompetitorChangeType changeType);
}
