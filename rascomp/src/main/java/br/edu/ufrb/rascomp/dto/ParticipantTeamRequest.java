package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantTeamRequest {
    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotNull
    private Long institutionId;
}
