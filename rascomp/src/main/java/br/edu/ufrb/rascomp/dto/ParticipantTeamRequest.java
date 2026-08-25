package br.edu.ufrb.rascomp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload usado pelo PARTICIPANTE para criar ou atualizar uma equipe própria.")
public class ParticipantTeamRequest {
    @NotBlank
    @Size(max = 120)
    @Schema(example = "Equipe Vespa")
    private String nome;

    @NotNull
    @Schema(example = "1", description = "ID da instituição à qual a equipe pertence.")
    private Long institutionId;
}
