package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findAllByOrderByDataCadastroDesc();
    List<Registration> findByAtivoTrueOrderByDataCadastroDesc();
    List<Registration> findByCompetitionIdOrderByDataCadastroDesc(Long competitionId);
    List<Registration> findByTeamIdOrderByDataCadastroDesc(Long teamId);
    List<Registration> findByTeamIdAndAtivoTrueOrderByDataCadastroDesc(Long teamId);
    List<Registration> findByRequestedByUserIdOrderByDataCadastroDesc(Long userId);
    List<Registration> findByStatusOrderByDataCadastroDesc(StatusRegistration status);
    List<Registration> findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
            Long competitionId,
            Long categoryId,
            StatusRegistration status);
    boolean existsByCompetitionIdAndCategoryIdAndRobotId(Long competitionId, Long categoryId, Long robotId);
    boolean existsByCompetitionIdAndCategoryIdAndRobotIdAndIdNot(Long competitionId, Long categoryId, Long robotId, Long id);
}
