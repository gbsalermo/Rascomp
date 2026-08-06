package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.MatchResult;
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
public class MatchResultDTO {
    private Long id;
    @NotNull private Long matchId;
    private Long winnerRegistrationId;
    private String winnerRobotNome;
    @NotNull @Min(0) private Integer pontosA;
    @NotNull @Min(0) private Integer pontosB;
    @Size(max = 500) private String observacao;
    private LocalDateTime dataCadastro;

    public MatchResultDTO(MatchResult entity) {
        this.id = entity.getId();
        this.matchId = entity.getMatch().getId();
        this.winnerRegistrationId = entity.getWinner() != null ? entity.getWinner().getId() : null;
        this.winnerRobotNome = entity.getWinner() != null ? entity.getWinner().getRobot().getNome() : null;
        this.pontosA = entity.getPontosA();
        this.pontosB = entity.getPontosB();
        this.observacao = entity.getObservacao();
        this.dataCadastro = entity.getDataCadastro();
    }
}
