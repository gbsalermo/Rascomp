package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.FollowTakeScheduleEntry;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowTakeScheduleEntryDTO {

    private Long id;
    private Long scheduleId;
    private Long competitionId;
    private Long categoryId;
    private Integer tomada;
    private Long registrationId;
    private String robotNome;
    private String teamNome;
    private String registrationStatus;

    @NotNull
    @Min(1)
    private Integer ordemConvocacao;

    @NotNull
    private StatusConvocacaoFollow status;

    private LocalDateTime dataCadastro;

    public FollowTakeScheduleEntryDTO(FollowTakeScheduleEntry entity) {
        this.id = entity.getId();
        this.scheduleId = entity.getSchedule().getId();
        this.competitionId = entity.getSchedule().getCompetition().getId();
        this.categoryId = entity.getSchedule().getCategory().getId();
        this.tomada = entity.getSchedule().getTomada();
        this.registrationId = entity.getRegistration().getId();
        this.robotNome = entity.getRegistration().getRobot().getNome();
        this.teamNome = entity.getRegistration().getTeam().getNome();
        this.registrationStatus = entity.getRegistration().getStatus().name();
        this.ordemConvocacao = entity.getOrdemConvocacao();
        this.status = entity.getStatus();
        this.dataCadastro = entity.getDataCadastro();
    }
}
