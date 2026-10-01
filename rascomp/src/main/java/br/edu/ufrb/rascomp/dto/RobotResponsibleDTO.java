package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RobotResponsible;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RobotResponsibleDTO {

    private Long id;
    private Long robotId;
    private String robotNome;
    private Long competitorId;
    private String competitorNome;
    private String competitorEmail;
    private Long userAccountId;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public RobotResponsibleDTO(RobotResponsible entity) {
        this.id = entity.getId();
        this.robotId = entity.getRobot().getId();
        this.robotNome = entity.getRobot().getNome();
        this.competitorId = entity.getCompetitor().getId();
        this.competitorNome = entity.getCompetitor().getNome();
        this.competitorEmail = entity.getCompetitor().getEmail();
        this.userAccountId = entity.getCompetitor().getUserAccount() == null
                ? null
                : entity.getCompetitor().getUserAccount().getId();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
