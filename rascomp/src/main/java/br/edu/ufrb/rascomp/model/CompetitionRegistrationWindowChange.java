package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.RegistrationWindowChangeType;
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
@Table(name = "competition_registration_window_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionRegistrationWindowChange implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationWindowChangeType tipo;

    @Column(name = "previous_end_date", nullable = false)
    private LocalDate dataFimAnterior;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate novaDataFim;

    @Column(nullable = false, length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by_user_id", nullable = false)
    private UserAccount realizadoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
