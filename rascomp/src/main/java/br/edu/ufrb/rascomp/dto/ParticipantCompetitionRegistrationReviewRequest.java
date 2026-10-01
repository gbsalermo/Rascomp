package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantCompetitionRegistrationReviewRequest {

    @NotNull
    private ParticipantCompetitionRegistrationStatus status;

    @Size(max = 500)
    private String motivo;
}
