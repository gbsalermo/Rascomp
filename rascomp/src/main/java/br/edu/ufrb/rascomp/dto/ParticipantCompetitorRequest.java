package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantCompetitorRequest {
    @NotBlank(message = "Nome do competidor é obrigatório")
    @Size(max = 150)
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String telefone;
}
