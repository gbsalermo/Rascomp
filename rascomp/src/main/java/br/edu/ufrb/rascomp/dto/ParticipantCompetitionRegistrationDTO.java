package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParticipantCompetitionRegistrationDTO {

    private Long id;
    private Long competitionId;
    private String competitionNome;
    private Long competitorId;
    private String competitorNome;
    private Long teamId;
    private String teamNome;
    private ParticipantCompetitionRegistrationStatus status;
    private String observacao;
    private Boolean comprovanteDisponivel;
    private String comprovanteNome;
    private Long requestedByUserId;
    private String requestedByUserNome;
    private Long reviewedByUserId;
    private String reviewedByUserNome;
    private LocalDateTime reviewedAt;
    private String reviewReason;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
    private List<ParticipantRobotLinkDTO> robots = new ArrayList<>();

    public ParticipantCompetitionRegistrationDTO(ParticipantCompetitionRegistration entity) {
        id = entity.getId();
        competitionId = entity.getCompetition().getId();
        competitionNome = entity.getCompetition().getNome();
        competitorId = entity.getCompetitor().getId();
        competitorNome = entity.getCompetitor().getNome();
        teamId = entity.getCompetitor().getTeam().getId();
        teamNome = entity.getCompetitor().getTeam().getNome();
        status = entity.getStatus();
        observacao = entity.getObservacao();
        comprovanteDisponivel = entity.getPaymentReceiptStorageKey() != null;
        comprovanteNome = entity.getPaymentReceiptOriginalName();
        requestedByUserId = entity.getRequestedByUser().getId();
        requestedByUserNome = entity.getRequestedByUser().getNome();
        if (entity.getReviewedByUser() != null) {
            reviewedByUserId = entity.getReviewedByUser().getId();
            reviewedByUserNome = entity.getReviewedByUser().getNome();
        }
        reviewedAt = entity.getReviewedAt();
        reviewReason = entity.getReviewReason();
        ativo = entity.getAtivo();
        dataCadastro = entity.getDataCadastro();
    }
}
