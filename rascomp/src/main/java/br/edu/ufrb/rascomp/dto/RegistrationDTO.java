package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
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
public class RegistrationDTO {
    private Long id;

    @NotNull
    private Long competitionId;
    private String competitionNome;
    private Long registrationLotId;
    private String registrationLotNome;

    @NotNull
    private Long categoryId;
    private String categoryNome;

    @NotNull
    private Long teamId;
    private String teamNome;

    @NotNull
    private Long robotId;
    private String robotNome;

    @Size(max = 500)
    private String robotDescricao;

    private List<Long> competitorIds;
    private List<String> competitorNomes;

    private Long requestedByUserId;
    private String requestedByUserNome;
    private Long reviewedByUserId;
    private String reviewedByUserNome;
    private LocalDateTime reviewedAt;

    @Size(max = 500)
    private String reviewReason;

    private StatusRegistration status;

    @Size(max = 500)
    private String observacao;

    private Boolean comprovanteDisponivel;
    private String comprovanteNome;

    private Boolean ativo;
    private LocalDateTime dataCadastro;

    public RegistrationDTO(Registration entity) {
        this.id = entity.getId();
        this.competitionId = entity.getCompetition().getId();
        this.competitionNome = entity.getCompetition().getNome();
        if (entity.getRegistrationLot() != null) {
            this.registrationLotId = entity.getRegistrationLot().getId();
            this.registrationLotNome = entity.getRegistrationLot().getNome();
        }
        this.categoryId = entity.getCategory().getId();
        this.categoryNome = entity.getCategory().getNome();
        this.teamId = entity.getTeam().getId();
        this.teamNome = entity.getTeam().getNome();
        this.robotId = entity.getRobot().getId();
        this.robotNome = entity.getRobot().getNome();
        this.robotDescricao = entity.getRobotDescription();

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
        this.reviewReason = entity.getReviewReason();
        this.status = entity.getStatus();
        this.observacao = entity.getObservacao();
        this.comprovanteDisponivel = entity.getPaymentReceiptStorageKey() != null;
        this.comprovanteNome = entity.getPaymentReceiptOriginalName();
        this.ativo = entity.getAtivo();
        this.dataCadastro = entity.getDataCadastro();
    }
}
