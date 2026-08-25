package br.edu.ufrb.rascomp.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload para inscrição enviada pelo PARTICIPANTE. O servidor define equipe, status PENDENTE e autoria a partir do JWT.")
public class ParticipantRegistrationRequest {
    @NotNull
    @Schema(example = "4")
    private Long competitionId;

    @NotNull
    @Schema(example = "3")
    private Long categoryId;

    @NotNull
    @Schema(example = "10", description = "Robô da própria equipe que será inscrito.")
    private Long robotId;

    @NotEmpty(message = "Informe ao menos um competidor")
    @Schema(example = "[7, 8]", description = "IDs dos competidores da mesma equipe que participarão da inscrição.")
    private List<@NotNull Long> competitorIds;

    @Size(max = 500)
    @Schema(example = "Equipe pronta para a competição.")
    private String observacao;
}
