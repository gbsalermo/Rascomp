package br.edu.ufrb.rascomp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Dados para criação de uma conta PARTICIPANTE.")
public class RegisterRequest {

    @NotBlank
    @Size(max = 150)
    @Schema(example = "Gabriel Salermo", description = "Nome exibido da conta.")
    private String nome;

    @NotBlank
    @Email
    @Size(max = 150)
    @Schema(example = "participante@exemplo.com", description = "E-mail único usado no login.")
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    @Schema(example = "SenhaForte@2026", format = "password", writeOnly = true,
            description = "Senha recebida apenas para autenticação; é persistida como hash BCrypt.")
    private String senha;

    @Size(max = 20)
    @Schema(example = "75999999999", description = "Telefone opcional do usuário.")
    private String telefone;
}
