package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Representação pública/autenticada de uma conta. Nunca contém senha ou passwordHash.")
public class UserAccountDTO {
    @Schema(example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    private final Long id;

    @Schema(example = "Gabriel Salermo", accessMode = Schema.AccessMode.READ_ONLY)
    private final String nome;

    @Schema(example = "participante@exemplo.com", accessMode = Schema.AccessMode.READ_ONLY)
    private final String email;

    @Schema(example = "75999999999", accessMode = Schema.AccessMode.READ_ONLY)
    private final String telefone;

    @Schema(example = "PARTICIPANTE", allowableValues = {"PARTICIPANTE", "ORGANIZACAO"}, accessMode = Schema.AccessMode.READ_ONLY)
    private final UserRole role;

    @Schema(example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private final Boolean ativo;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final LocalDateTime ultimoLogin;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private final LocalDateTime dataCadastro;

    public UserAccountDTO(UserAccount entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.email = entity.getEmail();
        this.telefone = entity.getTelefone();
        this.role = entity.getRole();
        this.ativo = entity.getAtivo();
        this.ultimoLogin = entity.getUltimoLogin();
        this.dataCadastro = entity.getDataCadastro();
    }
}
