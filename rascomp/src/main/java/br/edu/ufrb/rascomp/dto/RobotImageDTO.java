package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RobotImage;
import lombok.Getter;

@Getter
public class RobotImageDTO {
    private final Long id;
    private final Long robotId;
    private final String originalFilename;
    private final String contentType;
    private final Boolean principal;
    private final Integer ordem;
    private final String url;
    private final LocalDateTime dataCadastro;

    public RobotImageDTO(RobotImage entity) {
        this.id = entity.getId();
        this.robotId = entity.getRobot().getId();
        this.originalFilename = entity.getOriginalFilename();
        this.contentType = entity.getContentType();
        this.principal = entity.getPrincipal();
        this.ordem = entity.getOrdem();
        this.url = "/api/v1/public/robos/" + entity.getRobot().getId()
                + "/fotos/" + entity.getId() + "/arquivo";
        this.dataCadastro = entity.getDataCadastro();
    }
}
