package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import jakarta.validation.constraints.DecimalMin;
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
public class TentativaSeguidorLinhaDTO {
    private Long id;

    @NotNull
    private Long registrationId;

    private Long competitionId;
    private Long categoryId;
    private String teamNome;
    private String robotNome;

    @NotNull
    @Min(1)
    private Integer tomada;

    @NotNull
    @Min(1)
    private Integer numeroTentativa;

    @DecimalMin("0.0")
    private BigDecimal tempoSegundos;

    @NotNull
    @Min(0)
    private Integer checkpointsAlcancados;

    @NotNull
    @Min(0)
    private Integer penalidadeSegundos;

    private BigDecimal tempoFinalSegundos;

    @NotNull
    private Boolean concluida;

    @NotNull
    private Boolean valida;

    @Size(max = 500)
    private String observacao;

    private LocalDateTime dataCadastro;

    public TentativaSeguidorLinhaDTO(TentativaSeguidorLinha entity) {
        Registration registration = entity.getRegistration();

        this.id = entity.getId();
        this.registrationId = registration.getId();
        this.competitionId = registration.getCompetition() != null ? registration.getCompetition().getId() : null;
        this.categoryId = registration.getCategory() != null ? registration.getCategory().getId() : null;
        this.teamNome = registration.getTeam() != null ? registration.getTeam().getNome() : null;
        this.robotNome = registration.getRobot() != null ? registration.getRobot().getNome() : null;
        this.tomada = entity.getTomada();
        this.numeroTentativa = entity.getNumeroTentativa();
        this.tempoSegundos = entity.getTempoSegundos();
        this.checkpointsAlcancados = entity.getCheckpointsAlcancados();
        this.penalidadeSegundos = entity.getPenalidadeSegundos();

        if (entity.getTempoSegundos() != null) {
            int penalidade = entity.getPenalidadeSegundos() != null ? entity.getPenalidadeSegundos() : 0;
            this.tempoFinalSegundos = entity.getTempoSegundos().add(BigDecimal.valueOf(penalidade));
        }

        this.concluida = entity.getConcluida();
        this.valida = entity.getValida();
        this.observacao = entity.getObservacao();
        this.dataCadastro = entity.getDataCadastro();
    }
}
