package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Inscrição administrativa. Campos de autoria e revisão são preenchidos pelo servidor.")
public class RegistrationDTO {
    @Schema(example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(example = "4")
    private Long competitionId;

    @Schema(example = "RRC 2026", accessMode = Schema.AccessMode.READ_ONLY)
    private String competitionNome;

    @NotNull
    @Schema(example = "3")
    private Long categoryId;

    @Schema(example = "Seguidor de Linha", accessMode = Schema.AccessMode.READ_ONLY)
    private String categoryNome;

    @NotNull
    @Schema(example = "4")
    private Long teamId;

    @Schema(example = "Equipe Vespa", accessMode = Schema.AccessMode.READ_ONLY)
    private String teamNome;

    @NotNull
    @Schema(example = "10")
    private Long robotId;

    @Schema(example = "Vespa", accessMode = Schema.AccessMode.READ_ONLY)
    private String robotNome;

    @Schema(example = "[7, 8]", description = "Competidores da equipe efetivamente vinculados à inscrição.")
    private List<Long> competitorIds;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private List<String> competitorNomes;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long requestedByUserId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String requestedByUserNome;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long reviewedByUserId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String reviewedByUserNome;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime reviewedAt;

    @Schema(example = "PENDENTE",
            allowableValues = {"PENDENTE", "APROVADA", "REJEITADA", "CANCELADA", "DESCLASSIFICADA"},
            description = "No fluxo do PARTICIPANTE o status nasce PENDENTE; aprovação/rejeição é responsabilidade da ORGANIZACAO. DESCLASSIFICADA pode ser aplicada pelas regras competitivas.")
    private StatusRegistration status;

    @Size(max = 500)
    @Schema(example = "Documentação conferida pela organização.")
    private String observacao;

    @Schema(example = "true")
    private Boolean ativo;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime dataCadastro;

    public RegistrationDTO(Registration entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.robotId = entity.getRobot().getId();
        this.robotNome = entity.getRobot().getNome();

        this.competitorIds = entity.getCompetitors().stream()
                .map(competitor -> competitor.getId())
                .toList();
        this.competitorNomes = entity.getCompetitors().stream()
                .map(competitor -> competitor.getNome())
                .toList();

        if (entity.getRequestedByUser() != null) {
            this.requestedByUserId = entity.getRequestedByUser().getId();
            this.requestedByUserNome = entity.getRequestedByUser().getNome();
        }

        if (entity.getReviewedByUser() != null) {
            this.reviewedByUserId = entity.getReviewedByUser().getId();
            this.reviewedByUserNome = entity.getReviewedByUser().getNome();
        }

        this.reviewedAt = entity.getReviewedAt();
        this.status = entity.getStatus();
        this.observacao = entity.getObservacao();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
