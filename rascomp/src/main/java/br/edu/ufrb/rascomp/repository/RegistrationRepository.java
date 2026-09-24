package br.edu.ufrb.rascomp.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findAllByOrderByDataCadastroDesc();
    List<Registration> findByAtivoTrueOrderByDataCadastroDesc();
    List<Registration> findByCompetitionIdOrderByDataCadastroDesc(Long competitionId);
    List<Registration> findByTeamIdOrderByDataCadastroDesc(Long teamId);
    List<Registration> findByTeamIdAndAtivoTrueOrderByDataCadastroDesc(Long teamId);
    List<Registration> findByRequestedByUserIdOrderByDataCadastroDesc(Long userId);

    @Query("""
            select distinct r.team
            from Registration r
            where r.competition.id = :competitionId
            order by r.team.nome asc
            """)
    List<Team> findTeamsByCompetitionId(@Param("competitionId") Long competitionId);

    @Query("""
            select distinct r.robot
            from Registration r
            where r.competition.id = :competitionId
            order by r.robot.nome asc
            """)
    List<Robot> findRobotsByCompetitionId(@Param("competitionId") Long competitionId);

    @Query("""
            select distinct c
            from Registration r
            join r.competitors c
            where r.competition.id = :competitionId
            order by c.nome asc
            """)
    List<Competitor> findCompetitorsByCompetitionId(@Param("competitionId") Long competitionId);

    @Query("""
            select distinct r
            from Registration r
            join r.competitors c
            where r.team.id = :teamId
              and c.userAccount.id = :userAccountId
            order by r.dataCadastro desc
            """)
    List<Registration> findByTeamIdAndParticipantUserIdOrderByDataCadastroDesc(
            @Param("teamId") Long teamId,
            @Param("userAccountId") Long userAccountId);
    List<Registration> findByStatusOrderByDataCadastroDesc(StatusRegistration status);
    List<Registration> findByCompetitionIdAndRobotIdAndStatusIn(
            Long competitionId,
            Long robotId,
            Collection<StatusRegistration> statuses);
    List<Registration> findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
            Long competitionId,
            Long categoryId,
            StatusRegistration status);
    boolean existsByCompetitionIdAndRobotId(Long competitionId, Long robotId);
    boolean existsByCompetitionIdAndCategoryIdAndRobotId(Long competitionId, Long categoryId, Long robotId);
    boolean existsByCompetitionIdAndCategoryIdAndRobotIdAndIdNot(Long competitionId, Long categoryId, Long robotId, Long id);
}
