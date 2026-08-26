package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
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
    private Boolean bracketAtual;
    private Boolean bracketAtivo;
    private Long competitionId;
    private String competitionNome;
    private Long categoryId;
    private String categoryNome;
    @NotNull @Min(1) private Integer rodada;
    @NotNull @Min(1) private Integer ordem;
    private Long registrationAId;
    private Long robotAId;
    private String robotANome;
    private String teamANome;
    private Long registrationBId;
    private Long robotBId;
    private String robotBNome;
    private String teamBNome;
    private LocalDateTime dataHora;
    private StatusMatch status;
    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public MatchDTO(Match entity) {
        this.id = entity.getId();
        this.bracketId = entity.getBracket().getId();
        this.bracketNome = entity.getBracket().getNome();
        this.bracketAtual = entity.getBracket().getAtual();
        this.bracketAtivo = entity.getBracket().getAtivo();
        this.competitionId = entity.getBracket().getCompetition().getId();
        this.competitionNome = entity.getBracket().getCompetition().getNome();
        this.categoryId = entity.getBracket().getCategory().getId();
        this.categoryNome = entity.getBracket().getCategory().getNome();
        this.rodada = entity.getRodada();
        this.ordem = entity.getOrdem();
        preencherParticipanteA(entity.getRegistrationA());
        preencherParticipanteB(entity.getRegistrationB());
        this.dataHora = entity.getDataHora();
        this.status = entity.getStatus();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }

    private void preencherParticipanteA(Registration registration) {
        if (registration == null) return;
        this.registrationAId = registration.getId();
        this.robotAId = registration.getRobot().getId();
        this.robotANome = registration.getRobot().getNome();
        this.teamANome = registration.getTeam().getNome();
    }

    private void preencherParticipanteB(Registration registration) {
        if (registration == null) return;
        this.registrationBId = registration.getId();
        this.robotBId = registration.getRobot().getId();
        this.robotBNome = registration.getRobot().getNome();
        this.teamBNome = registration.getTeam().getNome();
    }
}
