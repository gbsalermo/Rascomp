package br.edu.ufrb.rascomp.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "config_sumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSumo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_category_id", nullable = false, unique = true)
    private CompetitionCategory competitionCategory;

    /** Referência informativa da classe; a inspeção humana decide APTO/INAPTO. */
    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal pesoMax;

    @Column(nullable = false)
    private Boolean exigeInspecao;

    @Column(nullable = false)
    private Integer maxTentativasInspecao;

    /** Quantidade de rounds regulares previstos para a batalha. */
    @Column(nullable = false)
    private Integer numeroRounds;

    /** Quantidade de vitórias necessárias para encerrar a batalha. */
    @Column(nullable = false)
    private Integer roundsParaVencer;

    /** Habilita rounds extras quando os regulares não produziram vencedor. */
    @Column(nullable = false)
    private Boolean permiteRoundDesempate;

    /** Limite de rounds extras disponíveis quando permitidos. */
    @Builder.Default
    @Column(name = "max_rounds_extras", nullable = false)
    private Integer maxRoundsExtras = 2;
}
