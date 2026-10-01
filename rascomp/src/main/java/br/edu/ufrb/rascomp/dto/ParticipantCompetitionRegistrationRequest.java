package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantCompetitionRegistrationRequest {

    @NotNull
    private Long competitionId;

    @Size(max = 500)
    private String observacao;
}
