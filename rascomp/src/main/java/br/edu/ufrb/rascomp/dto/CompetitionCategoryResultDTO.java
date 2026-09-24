package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionCategoryResultDTO {

    private Long categoryId;
    private String categoryNome;
    private Modalidade modalidade;
    private String status;

    private Long winnerRegistrationId;
    private String winnerRobotNome;
    private String winnerTeamNome;

    private BigDecimal tempoFinalSegundos;
    private Integer pontosA;
    private Integer pontosB;
    private Long finalMatchId;
}
