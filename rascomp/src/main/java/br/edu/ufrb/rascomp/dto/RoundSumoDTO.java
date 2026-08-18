package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
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
public class RoundSumoDTO {

    private Long id;

    @NotNull(message = "Partida é obrigatória")
    private Long matchId;

    private Integer numeroRound;

    private Long winnerRegistrationId;
    private String winnerRobotNome;

    @NotNull(message = "Status do round é obrigatório")
    private StatusRoundSumo status;

    @Size(max = 500)
    private String observacao;

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
