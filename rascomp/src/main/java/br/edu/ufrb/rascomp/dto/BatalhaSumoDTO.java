package br.edu.ufrb.rascomp.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatalhaSumoDTO {

    @NotNull(message = "Partida é obrigatória")
    private Long matchId;

    @Valid
    @NotEmpty(message = "Informe ao menos um round")
    private List<RoundSumoItemDTO> rounds = new ArrayList<>();
}
