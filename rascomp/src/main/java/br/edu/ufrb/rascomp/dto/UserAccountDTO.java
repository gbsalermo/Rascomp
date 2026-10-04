package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserAccountDTO {
    private final Long id;
    private final String nome;
    private final String email;
    private final String telefone;
    private final UserRole role;
    private final Boolean ativo;
    private final LocalDateTime ultimoLogin;
    private final LocalDateTime dataCadastro;

    @Setter
    private Long competitorId;
    @Setter
    private String competitorNome;
    @Setter
    private Long competitorTeamId;
    @Setter
    private String competitorTeamNome;
    @Setter
    private Boolean competitorAtivo;
    @Setter
    private Boolean teamWithoutActiveCompetitors;

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
