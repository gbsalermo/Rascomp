package br.edu.ufrb.rascomp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.RegistrationLot;

@Repository
public interface RegistrationLotRepository extends JpaRepository<RegistrationLot, Long> {

    List<RegistrationLot> findByCompetitionIdAndAtivoTrueOrderByDataInicioAscIdAsc(Long competitionId);

    Optional<RegistrationLot>
        findFirstByCompetitionIdAndAtivoTrueAndDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
            Long competitionId,
            LocalDate dataInicio,
            LocalDate dataFim);

    boolean existsByCompetitionIdAndAtivoTrue(Long competitionId);
}
