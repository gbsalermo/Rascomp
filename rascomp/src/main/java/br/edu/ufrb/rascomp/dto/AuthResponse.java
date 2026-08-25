package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Resposta de autenticação contendo o JWT e o usuário autenticado.")
public class AuthResponse {
    @Schema(description = "JWT usado em Authorization: Bearer <token>.", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String token;

    @Schema(example = "Bearer")
    private final String tipo = "Bearer";

    private final UserAccountDTO usuario;

    public AuthResponse(String token, UserAccount usuario) {
        this.token = token;
        this.usuario = new UserAccountDTO(usuario);
    }
}
