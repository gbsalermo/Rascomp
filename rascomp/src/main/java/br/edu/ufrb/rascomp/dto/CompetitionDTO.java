package br.edu.ufrb.rascomp.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
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
public class CompetitionDTO {
    private Long id;

    @NotBlank(message = "Nome da competição é obrigatório")
    @Size(max = 150)
    private String nome;

    @Size(max = 500)
    private String descricao;

    @NotNull
    private LocalDate inicioInscricoes;
    @NotNull
    private LocalDate fimInscricoes;
    @NotNull
    private LocalDate dataInicio;
    @NotNull
    private LocalDate dataFim;

    private StatusCompetition status;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public CompetitionDTO(Competition entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.inicioInscricoes = entity.getInicioInscricoes();
        this.fimInscricoes = entity.getFimInscricoes();
        this.dataInicio = entity.getDataInicio();
        this.dataFim = entity.getDataFim();
        this.status = entity.getStatus();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
