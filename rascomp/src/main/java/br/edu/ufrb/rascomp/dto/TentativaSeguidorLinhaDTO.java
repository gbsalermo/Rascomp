package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @NotNull private Long registrationId;
    @NotNull @Min(1) private Integer tomada;
    @NotNull @Min(1) private Integer numeroTentativa;
    @DecimalMin("0.0") private BigDecimal tempoSegundos;
    @NotNull @Min(0) private Integer checkpointsAlcancados;
    @NotNull @Min(0) private Integer penalidadeSegundos;
    @NotNull private Boolean concluida;
    @NotNull private Boolean valida;
    @Size(max = 500) private String observacao;
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
