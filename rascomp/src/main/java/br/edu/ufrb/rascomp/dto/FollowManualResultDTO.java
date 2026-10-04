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
    private final Long secondRegistrationId;
    private final String secondRobotNome;
    private final String secondTeamNome;
    private final Long thirdRegistrationId;
    private final String thirdRobotNome;
    private final String thirdTeamNome;
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
        this.secondRegistrationId = entity.getSecondRegistration() == null ? null : entity.getSecondRegistration().getId();
        this.secondRobotNome = entity.getSecondRegistration() == null ? null : entity.getSecondRegistration().getRobot().getNome();
        this.secondTeamNome = entity.getSecondRegistration() == null ? null : entity.getSecondRegistration().getTeam().getNome();
        this.thirdRegistrationId = entity.getThirdRegistration() == null ? null : entity.getThirdRegistration().getId();
        this.thirdRobotNome = entity.getThirdRegistration() == null ? null : entity.getThirdRegistration().getRobot().getNome();
        this.thirdTeamNome = entity.getThirdRegistration() == null ? null : entity.getThirdRegistration().getTeam().getNome();
        this.decidedByUserId = entity.getDecidedByUser().getId();
        this.decidedByUserNome = entity.getDecidedByUser().getNome();
        this.justificativa = entity.getJustificativa();
        this.dataCadastro = entity.getDataCadastro();
    }
}
