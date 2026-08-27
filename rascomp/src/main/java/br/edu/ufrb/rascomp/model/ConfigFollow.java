package br.edu.ufrb.rascomp.model;

import java.io.Serializable;

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
@Table(name = "config_follow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_category_id", nullable = false, unique = true)
    private CompetitionCategory competitionCategory;

    /** Quantidade de tomadas disponíveis para cada inscrição/robô. */
    @Column(nullable = false)
    private Integer numeroTomadas;

    /** Quantidade máxima de tentativas registráveis dentro de cada tomada. */
    @Column(nullable = false)
    private Integer tentativasPorTomada;

    /** Tempo máximo permitido para uma tentativa concluir o percurso. */
    @Column(nullable = false)
    private Integer maxTempoSegundos;

    /** Quantidade total de checkpoints existentes no percurso da categoria. */
    @Column(nullable = false)
    private Integer numeroCheckpoints;

    /*
     * O resultado bruto é persistido em TentativaSeguidorLinha.
     * RankingFollowService calcula sob demanda:
     * 1. somente tentativas válidas, concluídas e com tempo;
     * 2. a melhor tentativa de cada tomada;
     * 3. a melhor tomada da inscrição/robô;
     * 4. a ordenação pelo tempo final (tempo + penalidade).
     *
     * Checkpoints são dados operacionais e não alteram o ranking enquanto
     * o regulamento oficial não definir impacto competitivo específico.
     */
}
