package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.UserAccount;
import lombok.Getter;

@Getter
public class AuthResponse {
    private final String token;
    private final String tipo = "Bearer";
    private final UserAccountDTO usuario;

    public AuthResponse(String token, UserAccount usuario) {
        this.token = token;
        this.usuario = new UserAccountDTO(usuario);
    }
}
