package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Competitor;
import lombok.Getter;

@Getter
public class PublicCompetitorDTO {
    private final Long id;
    private final String nome;
    private final Long teamId;
    private final String teamNome;

    public PublicCompetitorDTO(Competitor entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
    }
}
