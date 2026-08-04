package br.edu.ufrb.rascomp.dto;



import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionCategoryDTO {
	
	private Long id;
	
	@NotBlank(message = "Nome é obrigatório")
	private String nome;
	
	private String descricao;
	
	@NotNull(message = "Modalidade é obrigatoria")
	private Modalidade modalidade;
	
	private Boolean ativo;
	
	public CompetitionCategoryDTO( CompetitionCategory entity) {
		
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.modalidade = entity.getModalidade();
		this.ativo = entity.getAtivo();
	}

}
