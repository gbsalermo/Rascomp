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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participant_registration_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRegistrationStatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_registration_id", nullable = false)
    private ParticipantCompetitionRegistration participantRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private ParticipantCompetitionRegistrationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private ParticipantCompetitionRegistrationStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccount actorUser;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
