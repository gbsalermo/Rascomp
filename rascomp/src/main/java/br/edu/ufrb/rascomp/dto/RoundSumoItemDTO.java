package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    private MotivoResultadoRoundSumo motivoResultado;

    @Min(value = 0, message = "Penalidades do robô A não podem ser negativas")
    @Max(value = 2, message = "Penalidades do robô A não podem passar de 2")
    private Integer penalidadesA;

    @Min(value = 0, message = "Penalidades do robô B não podem ser negativas")
    @Max(value = 2, message = "Penalidades do robô B não podem passar de 2")
    private Integer penalidadesB;

    @Size(max = 500)
    private String observacao;
}
