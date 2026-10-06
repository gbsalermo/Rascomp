package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Team;
import lombok.Getter;

@Getter
public class PublicTeamDTO {
    private final Long id;
    private final String nome;
    private final Long institutionId;
    private final String institutionNome;
    private final String institutionSigla;
    private final String logoUrl;

    public PublicTeamDTO(Team entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.institutionId = entity.getInstitution().getId();
        this.institutionNome = entity.getInstitution().getNome();
        this.institutionSigla = entity.getInstitution().getSigla();
        this.logoUrl = entity.getLogoStorageKey() == null || entity.getLogoStorageKey().isBlank()
                ? null
                : "/api/v1/public/equipes/" + entity.getId() + "/logo";
    }
}
