package br.edu.ufrb.rascomp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload usado pelo PARTICIPANTE para cadastrar ou editar um robô da própria equipe.")
public class ParticipantRobotRequest {
    @NotBlank(message = "Nome do robô é obrigatório")
    @Size(max = 120)
    @Schema(example = "Vespa")
    private String nome;

    @Size(max = 500)
    @Schema(example = "Robô seguidor de linha da equipe.")
    private String descricao;
}
