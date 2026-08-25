package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Robot;
import lombok.Getter;

@Getter
public class PublicRobotDTO {
    private final Long id;
    private final String nome;
    private final String descricao;
    private final Long teamId;
    private final String teamNome;
    private final String fotoPrincipalUrl;

    public PublicRobotDTO(Robot entity, String fotoPrincipalUrl) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.fotoPrincipalUrl = fotoPrincipalUrl;
    }
}
