package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchAgendaDTO {
    private LocalDateTime dataHora;

    @Size(max = 80)
    private String pista;

    @Min(1)
    private Integer ordemExecucao;

    private StatusConvocacaoPartida statusConvocacao;
}
