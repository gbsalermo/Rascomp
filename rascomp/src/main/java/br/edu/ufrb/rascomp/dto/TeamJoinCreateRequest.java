package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamJoinCreateRequest {

    @Size(max = 500)
    private String mensagem;
}
