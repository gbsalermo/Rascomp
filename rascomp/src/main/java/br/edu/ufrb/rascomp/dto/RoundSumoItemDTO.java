package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
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
public class RoundSumoItemDTO {

    private Long winnerRegistrationId;

    @NotNull(message = "Status do round é obrigatório")
    private StatusRoundSumo status;

    @Size(max = 500)
    private String observacao;
}
