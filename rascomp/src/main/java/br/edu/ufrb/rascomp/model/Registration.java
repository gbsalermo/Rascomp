package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "registrations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_registration_competition_category_robot",
        columnNames = {"competition_id", "category_id", "robot_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Registration implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CompetitionCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "registration_competitors",
        joinColumns = @JoinColumn(name = "registration_id"),
        inverseJoinColumns = @JoinColumn(name = "competitor_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_registration_competitor",
            columnNames = {"registration_id", "competitor_id"}
        )
    )
    private Set<Competitor> competitors = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private UserAccount requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserAccount reviewedByUser;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRegistration status;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
