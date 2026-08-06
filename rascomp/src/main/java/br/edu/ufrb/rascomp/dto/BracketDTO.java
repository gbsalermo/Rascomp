package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
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
public class BracketDTO {
    private Long id;
    @NotNull private Long competitionId;
    private String competitionNome;
    @NotNull private Long categoryId;
    private String categoryNome;
    @NotBlank @Size(max = 150) private String nome;
    private StatusBracket status;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public BracketDTO(Bracket entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.nome = entity.getNome();
        this.status = entity.getStatus();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
