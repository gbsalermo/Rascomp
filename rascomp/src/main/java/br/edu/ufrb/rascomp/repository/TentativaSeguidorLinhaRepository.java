package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;

@Repository
public interface TentativaSeguidorLinhaRepository extends JpaRepository<TentativaSeguidorLinha, Long> {
    List<TentativaSeguidorLinha> findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(Long registrationId);

    List<TentativaSeguidorLinha> findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
            Long competitionId,
            Long categoryId);

    boolean existsByRegistrationIdAndTomadaAndNumeroTentativa(Long registrationId, Integer tomada, Integer numeroTentativa);
    boolean existsByRegistrationIdAndTomadaAndNumeroTentativaAndIdNot(Long registrationId, Integer tomada, Integer numeroTentativa, Long id);
}
