package br.edu.ufrb.rascomp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.edu.ufrb.rascomp.model.TeamMembershipRequest;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipStatus;

@Repository
public interface TeamMembershipRequestRepository extends JpaRepository<TeamMembershipRequest, Long> {

    List<TeamMembershipRequest> findByParticipantUserIdOrderByDataCadastroDesc(Long participantUserId);

    List<TeamMembershipRequest> findByTeamIdOrderByDataCadastroDesc(Long teamId);

    List<TeamMembershipRequest> findByParticipantUserIdAndStatusOrderByDataCadastroDesc(
            Long participantUserId,
            TeamMembershipStatus status);

    List<TeamMembershipRequest> findByTeamIdAndStatusOrderByDataCadastroDesc(
            Long teamId,
            TeamMembershipStatus status);

    boolean existsByTeamIdAndParticipantUserIdAndStatus(
            Long teamId,
            Long participantUserId,
            TeamMembershipStatus status);
}
