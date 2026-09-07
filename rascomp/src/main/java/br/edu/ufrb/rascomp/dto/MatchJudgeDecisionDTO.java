package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.MatchJudgeDecision;
import jakarta.validation.constraints.NotBlank;
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
public class MatchJudgeDecisionDTO {

    private Long id;

    @NotNull(message = "Partida é obrigatória")
    private Long matchId;

    @NotNull(message = "Vencedor é obrigatório")
    private Long winnerRegistrationId;
    private String winnerRobotNome;

    @NotNull(message = "Juiz é obrigatório")
    private Long judgeId;
    private String judgeNome;

    @NotBlank(message = "Justificativa é obrigatória")
    @Size(max = 500)
    private String justificativa;

    private LocalDateTime dataCadastro;

    public MatchJudgeDecisionDTO(MatchJudgeDecision entity) {
        this.id = entity.getId();
        this.matchId = entity.getMatch().getId();
        this.winnerRegistrationId = entity.getWinner().getId();
        this.winnerRobotNome = entity.getWinner().getRobot() != null ? entity.getWinner().getRobot().getNome() : null;
        this.judgeId = entity.getJudge().getId();
        this.judgeNome = entity.getJudge().getNome();
        this.justificativa = entity.getJustificativa();
        this.dataCadastro = entity.getDataCadastro();
    }
}
