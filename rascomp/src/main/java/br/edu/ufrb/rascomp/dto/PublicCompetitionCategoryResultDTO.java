package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublicCompetitionCategoryResultDTO {

    private Long categoryId;
    private String categoryNome;
    private Modalidade modalidade;
    private String status;
    private Boolean podiumCompleto;

    private Long winnerRegistrationId;
    private String winnerRobotNome;
    private String winnerTeamNome;
    private BigDecimal winnerTempoFinalSegundos;

    private Long secondRegistrationId;
    private String secondRobotNome;
    private String secondTeamNome;
    private BigDecimal secondTempoFinalSegundos;

    private Long thirdRegistrationId;
    private String thirdRobotNome;
    private String thirdTeamNome;
    private BigDecimal thirdTempoFinalSegundos;

    public PublicCompetitionCategoryResultDTO(CompetitionCategoryResultDTO source) {
        this.categoryId = source.getCategoryId();
        this.categoryNome = source.getCategoryNome();
        this.modalidade = source.getModalidade();
        this.status = source.getStatus();

        this.winnerRegistrationId = source.getWinnerRegistrationId();
        this.winnerRobotNome = source.getWinnerRobotNome();
        this.winnerTeamNome = source.getWinnerTeamNome();
        this.winnerTempoFinalSegundos = source.getTempoFinalSegundos();

        this.secondRegistrationId = source.getSecondRegistrationId();
        this.secondRobotNome = source.getSecondRobotNome();
        this.secondTeamNome = source.getSecondTeamNome();
        this.secondTempoFinalSegundos = source.getSecondTempoFinalSegundos();

        this.thirdRegistrationId = source.getThirdRegistrationId();
        this.thirdRobotNome = source.getThirdRobotNome();
        this.thirdTeamNome = source.getThirdTeamNome();
        this.thirdTempoFinalSegundos = source.getThirdTempoFinalSegundos();

        this.podiumCompleto = temTexto(winnerRobotNome)
                && temTexto(secondRobotNome)
                && temTexto(thirdRobotNome);
    }

    private boolean temTexto(String value) {
        return value != null && !value.isBlank();
    }
}
