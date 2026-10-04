package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.FollowTakeSchedule;
import br.edu.ufrb.rascomp.model.Enum.StatusChamadaFollow;
import jakarta.validation.constraints.Min;
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
public class FollowTakeScheduleDTO {

    private Long id;

    @NotNull
    private Long competitionId;
    private String competitionNome;

    @NotNull
    private Long categoryId;
    private String categoryNome;

    @NotNull
    @Min(1)
    private Integer tomada;

    @NotNull
    private LocalDateTime dataHora;

    @Size(max = 80)
    private String pista;

    @Min(1)
    private Integer ordemExecucao;

    private StatusChamadaFollow status;
    private Boolean ativo;
    private Integer totalFila;
    private Integer concluidos;
    private Integer ausentes;
    private LocalDateTime dataCadastro;

    public FollowTakeScheduleDTO(FollowTakeSchedule entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.tomada = entity.getTomada();
        this.dataHora = entity.getDataHora();
        this.pista = entity.getPista();
        this.ordemExecucao = entity.getOrdemExecucao();
        this.status = entity.getStatus();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
