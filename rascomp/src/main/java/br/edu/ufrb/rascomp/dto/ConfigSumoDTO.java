package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import br.edu.ufrb.rascomp.model.ConfigSumo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Configuração da categoria SUMO.")
public class ConfigSumoDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "2")
    private Long categoryId;

    @NotNull(message = "Peso maximo é obrigatorio")
    @DecimalMin(value = "0.001", message = "Peso máximo deve ser maior que zero")
    @Schema(example = "3.000", description = "Peso máximo permitido para o robô na unidade adotada pela competição.")
    private BigDecimal pesoMax;

    @NotNull(message = "Informe se a inspeção é obrigatória")
    @Schema(example = "true")
    private Boolean exigeInspecao;

    @NotNull(message = "Máximo de tentativas de inspeção é obrigatório")
    @Min(value = 1, message = "Máximo de tentativas não pode ser negativo")
    @Schema(example = "3")
    private Integer maxTentativasInspecao;

    @NotNull(message = "Número de rounds é obrigatório")
    @Min(value = 1, message = "Deve existir pelo menos um round regular")
    @Schema(example = "3", description = "Quantidade de rounds regulares planejados por partida.")
    private Integer numeroRounds;

    @NotNull(message = "Quantidade de rounds para vencer é obrigatória")
    @Min(value = 1, message = "É necessario vencer pelo menos um round")
    @Schema(example = "2", description = "Quantidade de vitórias necessárias para finalizar a partida.")
    private Integer roundsParaVencer;

    @NotNull(message = "Informe se é permitido round adicional")
    @Schema(example = "true")
    private Boolean permiteRoundDesempate;

    public ConfigSumoDTO(ConfigSumo entity) {
        this.id = entity.getId();
        this.categoryId = entity.getCompetitionCategory().getId();
        this.pesoMax = entity.getPesoMax();
        this.exigeInspecao = entity.getExigeInspecao();
        this.maxTentativasInspecao = entity.getMaxTentativasInspecao();
        this.numeroRounds = entity.getNumeroRounds();
        this.roundsParaVencer = entity.getRoundsParaVencer();
        this.permiteRoundDesempate = entity.getPermiteRoundDesempate();
    }
}
