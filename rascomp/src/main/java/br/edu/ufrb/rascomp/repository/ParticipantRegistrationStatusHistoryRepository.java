package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.ParticipantRegistrationStatusHistory;

@Repository
public interface ParticipantRegistrationStatusHistoryRepository
        extends JpaRepository<ParticipantRegistrationStatusHistory, Long> {

    List<ParticipantRegistrationStatusHistory>
            findByParticipantRegistrationIdOrderByDataCadastroDesc(Long participantRegistrationId);
}
