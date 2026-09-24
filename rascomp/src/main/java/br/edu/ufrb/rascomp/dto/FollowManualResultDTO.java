package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.FollowManualResult;
import lombok.Getter;

@Getter
public class FollowManualResultDTO {

    private final Long id;
    private final Long competitionId;
    private final Long categoryId;
    private final Long winnerRegistrationId;
    private final String winnerRobotNome;
    private final String winnerTeamNome;
    private final Long decidedByUserId;
    private final String decidedByUserNome;
    private final String justificativa;
    private final LocalDateTime dataCadastro;

    public FollowManualResultDTO(FollowManualResult entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.categoryId = entity.getCategory().getId();
        this.winnerRegistrationId = entity.getWinnerRegistration().getId();
        this.winnerRobotNome = entity.getWinnerRegistration().getRobot().getNome();
        this.winnerTeamNome = entity.getWinnerRegistration().getTeam().getNome();
        this.decidedByUserId = entity.getDecidedByUser().getId();
        this.decidedByUserNome = entity.getDecidedByUser().getNome();
        this.justificativa = entity.getJustificativa();
        this.dataCadastro = entity.getDataCadastro();
    }
}
