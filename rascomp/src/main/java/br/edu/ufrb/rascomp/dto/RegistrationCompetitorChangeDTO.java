package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RegistrationCompetitorChange;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationCompetitorChangeDTO {

    private Long id;
    private Long registrationId;
    private Long competitionId;
    private Long robotId;
    private String robotNome;
    private Long competitorId;
    private String competitorNome;
    private RegistrationCompetitorChangeType changeType;
    private RegistrationCompetitorChangeStatus status;
    private Long actorUserId;
    private String actorUserNome;
    private Long reviewedByUserId;
    private String reviewedByUserNome;
    private LocalDateTime reviewedAt;
    private String reason;
    private LocalDateTime dataCadastro;

    public RegistrationCompetitorChangeDTO(RegistrationCompetitorChange entity) {
        id = entity.getId();
        registrationId = entity.getRegistration().getId();
        competitionId = entity.getRegistration().getCompetition().getId();
        robotId = entity.getRegistration().getRobot().getId();
        robotNome = entity.getRegistration().getRobot().getNome();
        competitorId = entity.getCompetitor().getId();
        competitorNome = entity.getCompetitor().getNome();
        changeType = entity.getChangeType();
        status = entity.getStatus();
        if (entity.getActorUser() != null) {
            actorUserId = entity.getActorUser().getId();
            actorUserNome = entity.getActorUser().getNome();
        }
        if (entity.getReviewedByUser() != null) {
            reviewedByUserId = entity.getReviewedByUser().getId();
            reviewedByUserNome = entity.getReviewedByUser().getNome();
        }
        reviewedAt = entity.getReviewedAt();
        reason = entity.getReason();
        dataCadastro = entity.getDataCadastro();
    }
}
