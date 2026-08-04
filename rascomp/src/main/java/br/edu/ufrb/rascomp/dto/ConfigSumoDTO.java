package br.edu.ufrb.rascomp.dto;

import java.math.BigDecimal;

import br.edu.ufrb.rascomp.model.ConfigSumo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSumoDTO {
	
	private Long id;
	private Long categoryId;
	
	
	@NotNull(message = "Peso maximo é obrigatorio")
	@DecimalMin(value = "0.001", message = "Peso máximo deve ser maior que zero")
	private BigDecimal pesoMax;
	
	@NotNull(message = "Informe se a inspeção é obrigatória")
	private Boolean exigeInspecao;
	
	@NotNull(message = "Máximo de tentativas de inspeção é obrigatório")
	@Min(value = 1, message = "Máximo de tentativas não pode ser negativo")
	private Integer maxTentativasInspecao;
	
	@NotNull(message = "Número de rounds é obrigatório")
	@Min(value = 1, message = "Deve existir pelo menos um round regular")
	private Integer numeroRounds;
	
	@NotNull(message = "Quantidade de rounds para vencer é obrigatória")
	@Min(value = 1, message = "É necessario vencer pelo menos um round")
	private Integer roundsParaVencer;
	
	@NotNull(message = "Informe se é permitido round adicional")
	private Boolean permiteRoundDesempate;
	
	public ConfigSumoDTO(ConfigSumo entity) {
		
		this.id = entity.getId();
		this.categoryId = entity.getCompetitionCategory().getId();
		this.pesoMax = entity.getPesoMax();
		this.exigeInspecao = entity.getExigeInspecao();
		this.maxTentativasInspecao = entity.getMaxTentativasInspecao();
		this.numeroRounds = entity.getNumeroRounds();
		this.roundsParaVencer = entity.getRoundsParaVencer();
		this.permiteRoundDesempate = entity.getPermiteRoundDesempate();
	}
	

}
