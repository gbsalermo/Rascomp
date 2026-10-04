package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamLeaderTransferRequest {

    @NotNull
    private Long competitionId;

    @NotNull
    private Long newResponsibleUserId;

    @Size(min = 3, max = 500)
    private String motivo;
}
