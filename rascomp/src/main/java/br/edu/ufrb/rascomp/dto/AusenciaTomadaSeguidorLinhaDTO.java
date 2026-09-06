package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.AusenciaTomadaSeguidorLinha;
import jakarta.validation.constraints.Min;
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
public class AusenciaTomadaSeguidorLinhaDTO {

    private Long id;

    @NotNull
    private Long registrationId;

    private Long competitionId;
    private Long categoryId;
    private String teamNome;
    private String robotNome;

    @NotNull
    @Min(1)
    private Integer tomada;

    @Size(max = 500)
    private String observacao;

    private Long registradoPorId;
    private String registradoPorNome;
    private LocalDateTime dataCadastro;

    public AusenciaTomadaSeguidorLinhaDTO(AusenciaTomadaSeguidorLinha entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.competitionId = entity.getRegistration().getCompetition().getId();
        this.categoryId = entity.getRegistration().getCategory().getId();
        this.teamNome = entity.getRegistration().getTeam().getNome();
        this.robotNome = entity.getRegistration().getRobot().getNome();
        this.tomada = entity.getTomada();
        this.observacao = entity.getObservacao();
        this.registradoPorId = entity.getRegistradoPor().getId();
        this.registradoPorNome = entity.getRegistradoPor().getNome();
        this.dataCadastro = entity.getDataCadastro();
    }
}
