package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
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
    name = "matches",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_match_bracket_rodada_ordem",
        columnNames = {"bracket_id", "rodada", "ordem"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bracket_id", nullable = false)
    private Bracket bracket;

    @Column(nullable = false)
    private Integer rodada;

    @Column(nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_a_id")
    private Registration registrationA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_b_id")
    private Registration registrationB;

    @Column
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMatch status;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
