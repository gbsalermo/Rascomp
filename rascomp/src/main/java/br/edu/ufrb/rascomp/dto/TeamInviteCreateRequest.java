package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamInviteCreateRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 500)
    private String mensagem;
}
