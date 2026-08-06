package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
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
public class RegistrationDTO {
    private Long id;
    @NotNull private Long competitionId;
    private String competitionNome;
    @NotNull private Long categoryId;
    private String categoryNome;
    @NotNull private Long teamId;
    private String teamNome;
    @NotNull private Long robotId;
    private String robotNome;
    private StatusRegistration status;
    @Size(max = 500) private String observacao;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public RegistrationDTO(Registration entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.robotId = entity.getRobot().getId();
        this.robotNome = entity.getRobot().getNome();
        this.status = entity.getStatus();
        this.observacao = entity.getObservacao();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
