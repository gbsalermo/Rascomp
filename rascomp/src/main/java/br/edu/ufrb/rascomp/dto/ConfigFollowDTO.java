package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.ConfigFollow;
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
public class ConfigFollowDTO {
	
	private Long id;
	
	private Long competitionCategoryId;
	
	@NotNull(message = "Número de tomadas é obrigatório")
	@Min(value = 1, message = "Deve existir pelo menos uma tomada")
	private Integer numeroTomadas;
	
	@NotNull(message = "Tentativas por ttomada é obrigatório")
	@Min(value = 1, message = "Deve existir pelo menos uma tentativa por tomada")
	private Integer tentativasPorTomada;
	
	@NotNull(message = "Tempo máximo é obrigatório")
	@Min(value = 1, message = "Tempo máximo deve ser maior que zero")
	private Integer maxTempoSegundos;
	
	@NotNull(message = "Número de checkpoints é obrigatório")
	@Min(value = 1, message = "Deve existir pelo menos um checkpoint")
	private Integer numeroCheckpoints;
	
	public ConfigFollowDTO(ConfigFollow entity) {
		
		this.id = entity.getId();
		this.competitionCategoryId = entity.getCompetitionCategory().getId();
		this.numeroTomadas = entity.getNumeroTomadas();
		this.tentativasPorTomada = entity.getTentativasPorTomada();
		this.maxTempoSegundos = entity.getMaxTempoSegundos();
		this.numeroCheckpoints = entity.getNumeroCheckpoints();
		
	}

}
