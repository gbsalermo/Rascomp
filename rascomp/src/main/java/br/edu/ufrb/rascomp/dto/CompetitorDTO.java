package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Competitor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorDTO {

	
	private Long id;
	
	@NotBlank(message = "Nome do competidor é obrigatório")
	@Size(max = 150, message = "Nome deve possuir no máximo 150 caracteres")
	private String nome;
	
	@NotBlank(message = "E-mail é obrigatório")
	@Email(message = "Informe um e-mail válido")
	@Size(max = 150, message = "E-mail deve possuir no máximo 150 caracteres")
	private String email;
	
	@Size(max = 20, message = "Telefone deve possuir no máximo 20 caracteres")
    private String telefone;
	
	@NotNull(message = "Equipe é obrigatória")
    private Long teamId;
	
	private String teamNome;
	private Long institutionId;
	private String institutionNome;
	private String institutionSigla;
	private Boolean ativo;
	private LocalDateTime dataCadastro;
	
	public CompetitorDTO(Competitor entity) {
		
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.email = entity.getEmail();
		this.telefone = entity.getTelefone();
		this.teamId = entity.getTeam().getId();
		this.teamNome = entity.getTeam().getNome();
		this.institutionId = entity.getTeam().getInstitution().getId();
		this.institutionNome = entity.getTeam().getInstitution().getNome();
		this.institutionSigla = entity.getTeam().getInstitution().getSigla();
		this.dataCadastro = entity.getDataCadastro();
	}
}
