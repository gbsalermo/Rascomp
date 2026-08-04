package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "config_sumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSumo implements Serializable{
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "competition_category_id", nullable = false, unique = true)
	private CompetitionCategory competitionCategory;
	
	@Column(nullable = false, precision = 8, scale = 3)
	private BigDecimal pesoMax;
	
	@Column(nullable = false)
	private Boolean exigeInspecao; 
	
	@Column(nullable = false)
	private Integer maxTentativasInspecao;
	
	@Column(nullable = false)
    private Integer numeroRounds;                // ex: 3

    @Column(nullable = false)
    private Integer roundsParaVencer;              // ex: 2 (melhor de 3)

    @Column(nullable = false)
    private Boolean permiteRoundDesempate;         // ex: true (4º round se empatar 1x1... ver observação abaixo)

  
    /*
     * TODO — módulo de resultados:
     *
     * Criar RoundSumo relacionado a Match para registrar:
     * - número do round;
     * - vencedor;
     * - motivo da vitória;
     * - status do round: finalizado, empatado, anulado ou cancelado.
     *
     * Regras definidas:
     * - numeroRounds representa somente os rounds regulares;
     * - roundsParaVencer define quantas vitórias encerram a batalha;
     * - o round de desempate é adicional;
     * - o round adicional ocorre quando algum round regular não produz
     *   resultado válido, por empate, anulação, cancelamento ou problema técnico;
     * - rounds sem vencedor não contam para roundsParaVencer.
     */
}
