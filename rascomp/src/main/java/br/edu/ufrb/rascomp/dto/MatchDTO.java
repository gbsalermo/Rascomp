package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
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
public class MatchDTO {
    private Long id;
    @NotNull private Long bracketId;
    private String bracketNome;
    @NotNull @Min(1) private Integer rodada;
    @NotNull @Min(1) private Integer ordem;
    private Long registrationAId;
    private String robotANome;
    private Long registrationBId;
    private String robotBNome;
    private LocalDateTime dataHora;
    private StatusMatch status;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public MatchDTO(Match entity) {
        this.id = entity.getId();
        this.bracketId = entity.getBracket().getId();
        this.bracketNome = entity.getBracket().getNome();
        this.rodada = entity.getRodada();
        this.ordem = entity.getOrdem();
        this.registrationAId = entity.getRegistrationA() != null ? entity.getRegistrationA().getId() : null;
        this.robotANome = entity.getRegistrationA() != null ? entity.getRegistrationA().getRobot().getNome() : null;
        this.registrationBId = entity.getRegistrationB() != null ? entity.getRegistrationB().getId() : null;
        this.robotBNome = entity.getRegistrationB() != null ? entity.getRegistrationB().getRobot().getNome() : null;
        this.dataHora = entity.getDataHora();
        this.status = entity.getStatus();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
