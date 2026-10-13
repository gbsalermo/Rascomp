package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenVerificationRequest {

    @NotBlank
    private String token;
}
