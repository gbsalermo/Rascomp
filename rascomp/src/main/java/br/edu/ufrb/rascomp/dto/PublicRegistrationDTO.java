package br.edu.ufrb.rascomp.dto;

import java.util.List;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import lombok.Getter;

@Getter
public class PublicRegistrationDTO {
    private final Long id;
    private final Long competitionId;
    private final String competitionNome;
    private final Long categoryId;
    private final String categoryNome;
    private final Long teamId;
    private final String teamNome;
    private final Long robotId;
    private final String robotNome;
    private final List<String> competidores;
    private final StatusRegistration status;

    public PublicRegistrationDTO(Registration entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.robotId = entity.getRobot().getId();
        this.robotNome = entity.getRobot().getNome();
        this.competidores = entity.getCompetitors().stream().map(c -> c.getNome()).toList();
        this.status = entity.getStatus();
    }
}
