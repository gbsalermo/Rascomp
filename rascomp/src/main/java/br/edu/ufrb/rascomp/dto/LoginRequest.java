package br.edu.ufrb.rascomp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Credenciais para autenticação no Rascomp.")
public class LoginRequest {
    @NotBlank
    @Email
    @Schema(example = "participante@exemplo.com")
    private String email;

    @NotBlank
    @Schema(example = "SenhaForte@2026", format = "password", writeOnly = true)
    private String senha;
}
