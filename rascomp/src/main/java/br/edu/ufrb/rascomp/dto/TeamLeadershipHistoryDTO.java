package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.TeamLeadershipHistory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamLeadershipHistoryDTO {

    private Long id;
    private Long teamId;
    private String teamNome;
    private Long competitionId;
    private String competitionNome;
    private Long previousUserId;
    private String previousUserNome;
    private Long newUserId;
    private String newUserNome;
    private Long changedByUserId;
    private String changedByUserNome;
    private String reason;
    private LocalDateTime dataCadastro;

    public TeamLeadershipHistoryDTO(TeamLeadershipHistory entity) {
        id = entity.getId();
        teamId = entity.getTeam().getId();
        teamNome = entity.getTeam().getNome();
        if (entity.getCompetition() != null) {
            competitionId = entity.getCompetition().getId();
            competitionNome = entity.getCompetition().getNome();
        }
        if (entity.getPreviousUser() != null) {
            previousUserId = entity.getPreviousUser().getId();
            previousUserNome = entity.getPreviousUser().getNome();
        }
        newUserId = entity.getNewUser().getId();
        newUserNome = entity.getNewUser().getNome();
        changedByUserId = entity.getChangedByUser().getId();
        changedByUserNome = entity.getChangedByUser().getNome();
        reason = entity.getReason();
        dataCadastro = entity.getDataCadastro();
    }
}
