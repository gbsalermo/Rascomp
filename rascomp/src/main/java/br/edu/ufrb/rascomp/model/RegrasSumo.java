package br.edu.ufrb.rascomp.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.ToString;

@Entity
@Table(name = "regras_sumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RegrasSumo implements Serializable{
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne
	@JoinColumn(name = "category_id", nullable = false, unique = true)
	private Category category;
	
	@Column(name = "peso_min")
	private Integer pesoMin;
	
	@Column(name = "peso_max")
	private Integer pesoMax;
	
	@Column(name = "exige_inspecao", nullable = false)
	private boolean exigeInspecao; 
	
	@Column(name = "max_tentativas_inspecao", nullable = false)
	private Integer maxTentativasInspecao;
	
	@Column(name = "numero_rounds", nullable = false)
    private Integer numeroRounds;                // ex: 3

    @Column(name = "rounds_para_vencer", nullable = false)
    private Integer roundsParaVencer;              // ex: 2 (melhor de 3)

    @Column(name = "permite_round_desempate", nullable = false)
    private boolean permiteRoundDesempate;         // ex: true (4º round se empatar 1x1... ver observação abaixo)

    /*
     * TODO (módulo de resultados/Registration):
     * Implementar entidade RoundSumo (match_id, numeroRound, vencedor_registration_id,
     * motivoVitoria [ex: SAIU_DA_ARENA, IMOBILIZACAO, DECISAO_JUIZ]) para registrar
     * o resultado de cada round de uma luta.
     *
     * Regra fixa do sumo (não é config de categoria, é regra universal do esporte):
     * se um robô sair da área de combate por conta própria, perde o round automaticamente.
     * Essa lógica entra no service que apura o resultado de cada round, não na entidade.
     *
     * Lógica de apuração da luta: um robô vence a luta ao atingir roundsParaVencer
     * vitórias. Se numeroRounds for ímpar (ex: 3) isso já resolve sozinho (melhor de 3).
     * Confirmar com o usuário se permiteRoundDesempate cobre o caso de empate em rounds
     * pares, ou se é sempre 3 rounds fixos e o 4º é exceção rara.
     */
}
