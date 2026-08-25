package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Round de uma partida SUMO. A consolidação dos rounds pode finalizar a partida e gerar MatchResult automaticamente.")
public class RoundSumoDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "5")
    private Long id;

    @NotNull(message = "Partida é obrigatória")
    @Schema(example = "9")
    private Long matchId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1",
            description = "Sequência do round dentro da partida, calculada pelo servidor.")
    private Integer numeroRound;

    @Schema(example = "14", description = "Inscrição vencedora. Pode ser nula em empate/anulação/cancelamento.")
    private Long winnerRegistrationId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "SumoBot")
    private String winnerRobotNome;

    @NotNull(message = "Status do round é obrigatório")
    @Schema(example = "FINALIZADO", allowableValues = {"FINALIZADO", "EMPATADO", "ANULADO", "CANCELADO"})
    private StatusRoundSumo status;

    @Size(max = 500)
    @Schema(example = "Round encerrado por saída da arena.")
    private String observacao;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime dataCadastro;

    public RoundSumoDTO(RoundSumo entity) {
        this.id = entity.getId();
        this.matchId = entity.getMatch().getId();
        this.numeroRound = entity.getNumeroRound();
        this.winnerRegistrationId = entity.getWinner() != null ? entity.getWinner().getId() : null;
        this.winnerRobotNome = entity.getWinner() != null ? entity.getWinner().getRobot().getNome() : null;
        this.status = entity.getStatus();
        this.observacao = entity.getObservacao();
        this.dataCadastro = entity.getDataCadastro();
    }
}
