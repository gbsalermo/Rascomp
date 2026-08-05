package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Institution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionDTO {
	
	private Long id;
	
	@NotBlank(message = "Nome é obrigatório")
	@Size(max = 150, message = "Nome deve possuir no máximo 150 caracteres")
	private String nome;
	
	@NotBlank(message = "Sigla é obrigatória")
	@Size(max = 20, message = "Sigla deve possuir no máximo 20 caracteres")
	private String sigla;
	
	@Size(max = 100, message = "Cidade deve possuir no máximo 100 caracteres")
	private String cidade;
	
	@Pattern( regexp = "^[A-Za-z]{2}$", message = "Estado deve possuir exatamente duas letras")
	private String estado;

	private Boolean ativo;
	
	private LocalDateTime dataCadastro;
	
	public InstitutionDTO(Institution entity) {
		
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.sigla = entity.getSigla();
		this.cidade = entity.getCidade();
		this.estado = entity.getEstado();
		this.ativo = entity.getAtivo();
		this.dataCadastro = entity.getDataCadastro();
	}
}
