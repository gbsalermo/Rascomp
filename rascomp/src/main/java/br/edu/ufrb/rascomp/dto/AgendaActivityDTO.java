package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendaActivityDTO {

    private String tipo;
    private Long sourceId;
    private Long competitionId;
    private String competitionNome;
    private Long categoryId;
    private String categoryNome;
    private Modalidade modalidade;
    private String titulo;
    private LocalDateTime dataHora;
    private String pista;
    private Integer ordemExecucao;
    private String status;
    private Integer tomada;
    private Long bracketId;
    private Long matchId;
    private Integer totalFila;
    private Integer concluidos;
    private Integer ausentes;
}
