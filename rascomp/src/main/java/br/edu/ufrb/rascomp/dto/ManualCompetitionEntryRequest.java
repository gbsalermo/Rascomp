package br.edu.ufrb.rascomp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualCompetitionEntryRequest {

    @NotNull
    private Long competitionId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long participantUserId;

    @NotBlank
    @Size(max = 120)
    private String robotNome;

    @Size(max = 500)
    private String robotDescricao;

    @NotBlank
    @Size(max = 500)
    private String justificativa;
}
