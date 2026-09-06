package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RegistrationCancellationRequest;
import br.edu.ufrb.rascomp.model.Enum.StatusCancellationRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationCancellationRequestDTO {
    private Long id;
    private Long registrationId;
    private Long competitionId;
    private String competitionNome;
    private String teamNome;
    private String robotNome;
    private Long requestedByUserId;
    private String requestedByUserNome;
    private StatusCancellationRequest status;
    private String motivo;
    private Long reviewedByUserId;
    private String reviewedByUserNome;
    private LocalDateTime reviewedAt;
    private String resposta;
    private LocalDateTime dataCadastro;

    public RegistrationCancellationRequestDTO(RegistrationCancellationRequest entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.competitionId = entity.getRegistration().getCompetition().getId();
        this.competitionNome = entity.getRegistration().getCompetition().getNome();
        this.teamNome = entity.getRegistration().getTeam().getNome();
        this.robotNome = entity.getRegistration().getRobot().getNome();
        this.requestedByUserId = entity.getRequestedByUser().getId();
        this.requestedByUserNome = entity.getRequestedByUser().getNome();
        this.status = entity.getStatus();
        this.motivo = entity.getMotivo();
        if (entity.getReviewedByUser() != null) {
            this.reviewedByUserId = entity.getReviewedByUser().getId();
            this.reviewedByUserNome = entity.getReviewedByUser().getNome();
        }
        this.reviewedAt = entity.getReviewedAt();
        this.resposta = entity.getResposta();
        this.dataCadastro = entity.getDataCadastro();
    }
}
