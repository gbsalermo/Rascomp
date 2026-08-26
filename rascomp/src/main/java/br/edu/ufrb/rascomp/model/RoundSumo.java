package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
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
    name = "rounds_sumo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_round_sumo_match_numero",
        columnNames = {"match_id", "numero_round"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundSumo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "numero_round", nullable = false)
    private Integer numeroRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_registration_id")
    private Registration winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRoundSumo status;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_resultado", nullable = false, length = 30)
    private MotivoResultadoRoundSumo motivoResultado = MotivoResultadoRoundSumo.DISPUTA;

    @Column(name = "penalidades_a", nullable = false)
    private Integer penalidadesA = 0;

    @Column(name = "penalidades_b", nullable = false)
    private Integer penalidadesB = 0;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
