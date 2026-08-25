package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.InspecaoSumo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(description = "Inspeção técnica de uma inscrição SUMO.")
public class InspecaoSumoDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "8")
    private Long id;

    @NotNull(message = "Inscrição é obrigatória")
    @Schema(example = "14")
    private Long registrationId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1",
            description = "Número sequencial da tentativa de inspeção, calculado pelo servidor.")
    private Integer numeroTentativa;

    @NotNull(message = "Peso medido é obrigatório")
    @DecimalMin(value = "0.001", message = "Peso medido deve ser maior que zero")
    @Schema(example = "2.950")
    private BigDecimal pesoMedido;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "true",
            description = "Resultado calculado conforme ConfigSumo e peso máximo.")
    private Boolean aprovada;

    @Size(max = 500, message = "Observação deve possuir no máximo 500 caracteres")
    @Schema(example = "Robô dentro do peso permitido.")
    private String observacao;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime dataCadastro;

    public InspecaoSumoDTO(InspecaoSumo entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.numeroTentativa = entity.getNumeroTentativa();
        this.pesoMedido = entity.getPesoMedido();
        this.aprovada = entity.getAprovada();
        this.observacao = entity.getObservacao();
        this.dataCadastro = entity.getDataCadastro();
    }
}
