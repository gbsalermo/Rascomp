package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.AusenciaTomadaSeguidorLinha;

@Repository
public interface AusenciaTomadaSeguidorLinhaRepository extends JpaRepository<AusenciaTomadaSeguidorLinha, Long> {

    List<AusenciaTomadaSeguidorLinha> findByRegistrationIdOrderByTomadaAsc(Long registrationId);

    List<AusenciaTomadaSeguidorLinha> findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
            Long competitionId,
            Long categoryId);

    boolean existsByRegistrationId(Long registrationId);
    boolean existsByRegistrationCompetitionId(Long competitionId);
    boolean existsByRegistrationIdAndTomada(Long registrationId, Integer tomada);
}
