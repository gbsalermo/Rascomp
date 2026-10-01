package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.TeamMembershipRequest;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipRequestType;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamMembershipRequestDTO {

    private Long id;

    private Long teamId;
    private String teamNome;
    private String institutionNome;
    private String institutionSigla;

    private Long participantUserId;
    private String participantNome;
    private String participantEmail;

    private Long requestedByUserId;
    private String requestedByNome;

    private TeamMembershipRequestType requestType;
    private TeamMembershipStatus status;
    private String mensagem;

    private Long reviewedByUserId;
    private String reviewedByNome;
    private LocalDateTime reviewedAt;
    private LocalDateTime dataCadastro;

    public TeamMembershipRequestDTO(TeamMembershipRequest entity) {
        this.id = entity.getId();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.institutionNome = entity.getTeam().getInstitution().getNome();
        this.institutionSigla = entity.getTeam().getInstitution().getSigla();

        this.participantUserId = entity.getParticipantUser().getId();
        this.participantNome = entity.getParticipantUser().getNome();
        this.participantEmail = entity.getParticipantUser().getEmail();

        this.requestedByUserId = entity.getRequestedByUser().getId();
        this.requestedByNome = entity.getRequestedByUser().getNome();

        this.requestType = entity.getRequestType();
        this.status = entity.getStatus();
        this.mensagem = entity.getMensagem();

        if (entity.getReviewedByUser() != null) {
            this.reviewedByUserId = entity.getReviewedByUser().getId();
            this.reviewedByNome = entity.getReviewedByUser().getNome();
        }
        this.reviewedAt = entity.getReviewedAt();
        this.dataCadastro = entity.getDataCadastro();
    }
}
