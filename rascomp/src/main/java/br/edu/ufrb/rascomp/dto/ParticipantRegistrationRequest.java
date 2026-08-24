package br.edu.ufrb.rascomp.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantRegistrationRequest {
    @NotNull
    private Long competitionId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long robotId;

    @NotEmpty(message = "Informe ao menos um competidor")
    private List<@NotNull Long> competitorIds;

    @Size(max = 500)
    private String observacao;
}
