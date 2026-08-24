package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantRobotRequest {
    @NotBlank(message = "Nome do robô é obrigatório")
    @Size(max = 120)
    private String nome;

    @Size(max = 500)
    private String descricao;
}
