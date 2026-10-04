package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "participant_competition_registrations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_participant_competition_registration",
        columnNames = {"competition_id", "competitor_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantCompetitionRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserAccount requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserAccount reviewedByUser;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantCompetitionRegistrationStatus status;

    @Column(length = 500)
    private String observacao;

    @Column(name = "payment_receipt_storage_key", length = 500)
    private String paymentReceiptStorageKey;

    @Column(name = "payment_receipt_original_name", length = 255)
    private String paymentReceiptOriginalName;

    @Column(name = "payment_receipt_content_type", length = 100)
    private String paymentReceiptContentType;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
