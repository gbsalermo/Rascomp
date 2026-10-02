package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.TeamLeadershipHistory;

@Repository
public interface TeamLeadershipHistoryRepository extends JpaRepository<TeamLeadershipHistory, Long> {
    List<TeamLeadershipHistory> findByTeamIdOrderByDataCadastroDesc(Long teamId);
}
