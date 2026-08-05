package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompetitionCategoryRepository competitionCategoryRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final ConfigFollowRepository configFollowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (competitionCategoryRepository.count() > 0) {
            System.out.println("Dados de teste já existem. Inicialização ignorada.");
            return;
        }

        CompetitionCategory miniSumo = criarCategoria(
                "Mini Sumô",
                "Categoria de robôs de Sumô com peso máximo de 500 gramas.",
                Modalidade.SUMO,
                true
        );

        CompetitionCategory sumoTresKg = criarCategoria(
                "Sumô 3 kg",
                "Categoria de robôs de Sumô com peso máximo de 3 kg.",
                Modalidade.SUMO,
                true
        );

        CompetitionCategory seguidorLinha = criarCategoria(
                "Seguidor de Linha",
                "Categoria de robôs seguidores de linha.",
                Modalidade.FOLLOW_LINE,
                true
        );

        CompetitionCategory categoriaInativa = criarCategoria(
                "Sumô Experimental",
                "Categoria inativa criada para testar validações.",
                Modalidade.SUMO,
                false
        );

        criarConfigSumo(miniSumo, new BigDecimal("0.500"));
        criarConfigSumo(sumoTresKg, new BigDecimal("3.000"));
        criarConfigFollow(seguidorLinha);

        System.out.println("Dados de teste criados com sucesso.");
        System.out.println("Categorias criadas:");
        System.out.println("- " + miniSumo.getId() + " - Mini Sumô");
        System.out.println("- " + sumoTresKg.getId() + " - Sumô 3 kg");
        System.out.println("- " + seguidorLinha.getId() + " - Seguidor de Linha");
        System.out.println("- " + categoriaInativa.getId() + " - Sumô Experimental (inativa)");
    }

    private CompetitionCategory criarCategoria(
            String nome,
            String descricao,
            Modalidade modalidade,
            boolean ativo) {

        CompetitionCategory category = CompetitionCategory.builder()
                .nome(nome)
                .descricao(descricao)
                .modalidade(modalidade)
                .ativo(ativo)
                .build();

        return competitionCategoryRepository.save(category);
    }

    private void criarConfigSumo(
            CompetitionCategory category,
            BigDecimal pesoMax) {

        ConfigSumo config = ConfigSumo.builder()
                .competitionCategory(category)
                .pesoMax(pesoMax)
                .exigeInspecao(true)
                .maxTentativasInspecao(3)
                .numeroRounds(3)
                .roundsParaVencer(2)
                .permiteRoundDesempate(true)
                .build();

        configSumoRepository.save(config);
    }

    private void criarConfigFollow(CompetitionCategory category) {
        ConfigFollow config = ConfigFollow.builder()
                .competitionCategory(category)
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(180)
                .numeroCheckpoints(5)
                .build();

        configFollowRepository.save(config);
    }
}
