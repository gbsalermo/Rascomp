package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RegistrationCancellationRequest;
import br.edu.ufrb.rascomp.model.Enum.StatusCancellationRequest;

@Repository
public interface RegistrationCancellationRequestRepository extends JpaRepository<RegistrationCancellationRequest, Long> {
    boolean existsByRegistrationIdAndStatus(Long registrationId, StatusCancellationRequest status);

    List<RegistrationCancellationRequest> findByRegistrationIdOrderByDataCadastroDesc(Long registrationId);

    List<RegistrationCancellationRequest> findByStatusOrderByDataCadastroDesc(StatusCancellationRequest status);

    List<RegistrationCancellationRequest> findByRegistrationCompetitionIdOrderByDataCadastroDesc(Long competitionId);

    List<RegistrationCancellationRequest> findByRegistrationCompetitionIdAndStatusOrderByDataCadastroDesc(
            Long competitionId,
            StatusCancellationRequest status);
}
