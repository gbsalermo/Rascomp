package br.edu.ufrb.rascomp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.InspecaoSumo;

@Repository
public interface InspecaoSumoRepository extends JpaRepository<InspecaoSumo, Long> {

    List<InspecaoSumo> findByRegistrationIdOrderByNumeroTentativaAsc(Long registrationId);

    Optional<InspecaoSumo> findFirstByRegistrationIdOrderByNumeroTentativaDesc(Long registrationId);

    boolean existsByRegistrationIdAndAprovadaTrue(Long registrationId);

    long countByRegistrationId(Long registrationId);
}
