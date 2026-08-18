package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "inspecoes_sumo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inspecao_sumo_registration_tentativa",
        columnNames = {"registration_id", "numero_tentativa"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InspecaoSumo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @Column(name = "numero_tentativa", nullable = false)
    private Integer numeroTentativa;

    @Column(name = "peso_medido", nullable = false, precision = 8, scale = 3)
    private BigDecimal pesoMedido;

    @Column(nullable = false)
    private Boolean aprovada;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
}
