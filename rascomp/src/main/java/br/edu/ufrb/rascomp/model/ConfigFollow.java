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
@Table(name = "parametros_follow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ConfigFollow implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne
	@JoinColumn(name = "category_id", nullable = false, unique = true)
	private CompetitionCategory category;
	
	@Column(name = "numero_tomadas", nullable = false)
	private Integer numeroTomadas;  //ex: 3 tomadas/tentativas por robo
	
	@Column(name = "tentativas_por_tomadas")
	private Integer tentativasPorTomada; //ex: os 3 melhoresTempos
	
	
	}

/*
 * TODO (módulo de resultados/Registration):
 * Implementar entidade TentativaSeguidorLinha (registration_id, numeroTomada,
 * numeroTentativa, completou, tempoRegistrado) para registrar o resultado bruto
 * de cada tentativa do robô.
 *
 * Implementar método de cálculo (ex: PontuacaoSeguidorLinhaService) que:
 * 1. Agrupa as tentativas por numeroTomada
 * 2. Em cada tomada, pega o menor tempoRegistrado entre as tentativas com completou = true
 *    (se nenhuma tentativa da tomada foi completada, a tomada não gera tempo)
 * 3. Entre os tempos de tomada válidos, pega o menor de todos — esse é o tempo final do robô
 *
 * Decisão de implementação: calcular sob demanda a cada consulta de ranking
 * (mais simples e sempre correto para o volume esperado da competição),
 * ao invés de cachear um campo de "melhor tempo" na Registration.
 */

