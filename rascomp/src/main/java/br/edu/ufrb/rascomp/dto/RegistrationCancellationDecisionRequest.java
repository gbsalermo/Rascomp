package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationCancellationDecisionRequest {
    @Size(max = 500)
    private String resposta;
}
