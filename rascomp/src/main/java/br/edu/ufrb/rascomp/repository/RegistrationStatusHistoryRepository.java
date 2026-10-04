package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RegistrationStatusHistory;

@Repository
public interface RegistrationStatusHistoryRepository extends JpaRepository<RegistrationStatusHistory, Long> {

    List<RegistrationStatusHistory> findByRegistrationIdOrderByDataCadastroDesc(Long registrationId);
}
