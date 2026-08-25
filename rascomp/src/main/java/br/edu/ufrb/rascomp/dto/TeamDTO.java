package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamDTO {

    private Long id;

    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotNull
    private Long institutionId;

    private String institutionNome;
    private String institutionSigla;

    private Long responsibleUserId;
    private String responsibleUserNome;
    private String responsibleUserEmail;

    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public TeamDTO(Team entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.institutionId = entity.getInstitution().getId();
        this.institutionNome = entity.getInstitution().getNome();
        this.institutionSigla = entity.getInstitution().getSigla();

        if (entity.getResponsibleUser() != null) {
            this.responsibleUserId = entity.getResponsibleUser().getId();
            this.responsibleUserNome = entity.getResponsibleUser().getNome();
            this.responsibleUserEmail = entity.getResponsibleUser().getEmail();
        }

        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
