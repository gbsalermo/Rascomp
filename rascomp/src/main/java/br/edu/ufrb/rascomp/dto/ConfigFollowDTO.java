package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.ConfigFollow;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Configuração da categoria FOLLOW_LINE.")
public class ConfigFollowDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "3")
    private Long competitionCategoryId;

    @NotNull(message = "Número de tomadas é obrigatório")
    @Min(value = 1, message = "Deve existir pelo menos uma tomada")
    @Schema(example = "3", description = "Quantidade de tomadas disponíveis para cada inscrição.")
    private Integer numeroTomadas;

    @NotNull(message = "Tentativas por ttomada é obrigatório")
    @Min(value = 1, message = "Deve existir pelo menos uma tentativa por tomada")
    @Schema(example = "3", description = "Quantidade máxima de tentativas em cada tomada.")
    private Integer tentativasPorTomada;

    @NotNull(message = "Tempo máximo é obrigatório")
    @Min(value = 1, message = "Tempo máximo deve ser maior que zero")
    @Schema(example = "180", description = "Tempo máximo permitido para uma tentativa, em segundos.")
    private Integer maxTempoSegundos;

    @NotNull(message = "Número de checkpoints é obrigatório")
    @Min(value = 1, message = "Deve existir pelo menos um checkpoint")
    @Schema(example = "5", description = "Quantidade total de checkpoints da pista.")
    private Integer numeroCheckpoints;

    public ConfigFollowDTO(ConfigFollow entity) {
        this.id = entity.getId();
        this.competitionCategoryId = entity.getCompetitionCategory().getId();
        this.numeroTomadas = entity.getNumeroTomadas();
        this.tentativasPorTomada = entity.getTentativasPorTomada();
        this.maxTempoSegundos = entity.getMaxTempoSegundos();
        this.numeroCheckpoints = entity.getNumeroCheckpoints();
    }
}
