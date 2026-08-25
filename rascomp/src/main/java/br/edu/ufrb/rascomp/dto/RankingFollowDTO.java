package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Linha do ranking FOLLOW_LINE baseada na melhor tentativa válida e concluída da inscrição.")
public class RankingFollowDTO {
    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer posicao;

    @Schema(example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    private Long registrationId;

    @Schema(example = "10", accessMode = Schema.AccessMode.READ_ONLY)
    private Long robotId;

    @Schema(example = "Vespa", accessMode = Schema.AccessMode.READ_ONLY)
    private String robotNome;

    @Schema(example = "Equipe Vespa", accessMode = Schema.AccessMode.READ_ONLY)
    private String teamNome;

    @Schema(example = "38.500", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal tempoBrutoSegundos;

    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer penalidadeSegundos;

    @Schema(example = "39.500", description = "tempoBrutoSegundos + penalidadeSegundos", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal tempoFinalSegundos;

    @Schema(example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer tomada;

    @Schema(example = "2", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer numeroTentativa;
}
