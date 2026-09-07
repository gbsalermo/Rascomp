package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.CompetitionJudge;
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
public class CompetitionJudgeDTO {

    private Long id;

    @NotNull(message = "Competição é obrigatória")
    private Long competitionId;

    @NotBlank(message = "Nome do juiz é obrigatório")
    @Size(max = 150)
    private String nome;

    private Long userAccountId;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public CompetitionJudgeDTO(CompetitionJudge entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.nome = entity.getNome();
        this.userAccountId = entity.getUserAccount() != null ? entity.getUserAccount().getId() : null;
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
