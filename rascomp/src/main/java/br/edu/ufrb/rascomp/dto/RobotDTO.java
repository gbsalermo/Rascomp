package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Robot;
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
public class RobotDTO {
	
	private Long id;
	
	@NotBlank(message = "Nome do robô é obrigatório")
    @Size(max = 120, message = "Nome do robô deve possuir no máximo 120 caracteres")
    private String nome;
	
	@Size(max = 500, message = "Descrição deve possuir no máximo 500 caracteres")
    private String descricao;
	
	@NotNull(message = "Equipe é obrigatória")
    private Long teamId;
	
	private String teamNome;
    private Long institutionId;
    private String institutionNome;
    private String institutionSigla;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
    
    public RobotDTO(Robot entity) {
    	this.id = entity.getId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.institutionId = entity.getTeam().getInstitution().getId();
        this.institutionNome = entity.getTeam().getInstitution().getNome();
        this.institutionSigla = entity.getTeam().getInstitution().getSigla();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }

}
