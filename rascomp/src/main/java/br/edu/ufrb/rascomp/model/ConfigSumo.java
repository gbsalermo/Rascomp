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

    /** Permite no máximo um round adicional quando ainda não há vencedor. */
    @Column(nullable = false)
    private Boolean permiteRoundDesempate;

    /*
     * RoundSumo registra cada round e MatchResultService consolida a batalha.
     * Regras atuais relevantes:
     * - rounds sem vencedor não contam para roundsParaVencer;
     * - SUICIDIO_WO encerra o round com vitória do adversário;
     * - 2 penalidades no mesmo round causam derrota automática do robô penalizado;
     * - o round adicional só é aceito quando configurado e a batalha ainda não
     *   possui vencedor.
     *
     * Mini/3 kg e RC/Autônomo permanecem categorias distintas usando este
     * mesmo motor de Sumô; não são modalidades técnicas diferentes no backend.
     */
}
