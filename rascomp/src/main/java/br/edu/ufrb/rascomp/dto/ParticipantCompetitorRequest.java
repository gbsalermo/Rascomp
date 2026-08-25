package br.edu.ufrb.rascomp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload usado pelo PARTICIPANTE para cadastrar ou editar um competidor da própria equipe.")
public class ParticipantCompetitorRequest {
    @NotBlank(message = "Nome do competidor é obrigatório")
    @Size(max = 150)
    @Schema(example = "Ana Souza")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email
    @Size(max = 150)
    @Schema(example = "ana.souza@exemplo.com")
    private String email;

    @Size(max = 20)
    @Schema(example = "75988887777")
    private String telefone;
}
