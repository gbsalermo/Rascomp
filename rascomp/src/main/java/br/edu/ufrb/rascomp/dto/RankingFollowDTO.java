package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingFollowDTO {
    private Integer posicao;
    private Long registrationId;
    private Long robotId;
    private String robotNome;
    private String teamNome;
    private BigDecimal tempoBrutoSegundos;
    private Integer penalidadeSegundos;
    private BigDecimal tempoFinalSegundos;
    private Integer tomada;
    private Integer numeroTentativa;
}
