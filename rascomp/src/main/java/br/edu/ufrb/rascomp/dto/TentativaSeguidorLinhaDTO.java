package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Tentativa de uma inscrição na modalidade FOLLOW_LINE.")
public class TentativaSeguidorLinhaDTO {
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "21")
    private Long id;

    @NotNull
    @Schema(example = "12")
    private Long registrationId;

    @NotNull
    @Min(1)
    @Schema(example = "1", description = "Número da tomada configurada para a categoria.")
    private Integer tomada;

    @NotNull
    @Min(1)
    @Schema(example = "2", description = "Número da tentativa dentro da tomada.")
    private Integer numeroTentativa;

    @DecimalMin("0.0")
    @Schema(example = "38.500", description = "Tempo bruto medido em segundos.")
    private BigDecimal tempoSegundos;

    @NotNull
    @Min(0)
    @Schema(example = "5")
    private Integer checkpointsAlcancados;

    @NotNull
    @Min(0)
    @Schema(example = "1", description = "Penalidade somada ao tempo bruto, em segundos.")
    private Integer penalidadeSegundos;

    @NotNull
    @Schema(example = "true")
    private Boolean concluida;

    @NotNull
    @Schema(example = "true", description = "Tentativas inválidas não entram no ranking.")
    private Boolean valida;

    @Size(max = 500)
    @Schema(example = "Concluiu a pista sem incidentes.")
    private String observacao;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime dataCadastro;

    public TentativaSeguidorLinhaDTO(TentativaSeguidorLinha entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.tomada = entity.getTomada();
        this.numeroTentativa = entity.getNumeroTentativa();
        this.tempoSegundos = entity.getTempoSegundos();
        this.checkpointsAlcancados = entity.getCheckpointsAlcancados();
        this.penalidadeSegundos = entity.getPenalidadeSegundos();
        this.concluida = entity.getConcluida();
        this.valida = entity.getValida();
        this.observacao = entity.getObservacao();
        this.dataCadastro = entity.getDataCadastro();
    }
}
