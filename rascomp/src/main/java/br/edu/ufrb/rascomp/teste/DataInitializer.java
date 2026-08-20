package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompetitionCategoryRepository competitionCategoryRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final CompetitionRepository competitionRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaSeguidorLinhaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (competitionCategoryRepository.count() > 0) {
            System.out.println("Dados de teste já existem. Inicialização ignorada.");
            return;
        }

        // Categorias e configurações
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

        criarCategoria(
                "Sumô Experimental",
                "Categoria inativa criada para testar validações.",
                Modalidade.SUMO,
                false
        );

        criarConfigSumo(miniSumo, new BigDecimal("0.500"));
        criarConfigSumo(sumoTresKg, new BigDecimal("3.000"));
        criarConfigFollow(seguidorLinha);

        // Instituições
        Institution ufrb = new Institution();
        ufrb.setNome("Universidade Federal do Recôncavo da Bahia");
        ufrb.setSigla("UFRB");
        ufrb.setCidade("Cruz das Almas");
        ufrb.setEstado("BA");
        ufrb.setAtivo(true);
        ufrb = institutionRepository.save(ufrb);

        Institution ifba = new Institution();
        ifba.setNome("Instituto Federal da Bahia");
        ifba.setSigla("IFBA");
        ifba.setCidade("Salvador");
        ifba.setEstado("BA");
        ifba.setAtivo(true);
        ifba = institutionRepository.save(ifba);

        // Equipes
        Team rasUfrb = new Team();
        rasUfrb.setNome("RAS UFRB");
        rasUfrb.setInstitution(ufrb);
        rasUfrb.setAtivo(true);
        rasUfrb = teamRepository.save(rasUfrb);

        Team equipeIfba = new Team();
        equipeIfba.setNome("Robótica IFBA");
        equipeIfba.setInstitution(ifba);
        equipeIfba.setAtivo(true);
        equipeIfba = teamRepository.save(equipeIfba);

        // Competidores
        Competitor gabriel = new Competitor();
        gabriel.setNome("Gabriel Salermo");
        gabriel.setEmail("gabriel@rascomp.dev");
        gabriel.setTelefone("75999990001");
        gabriel.setTeam(rasUfrb);
        gabriel.setAtivo(true);
        gabriel = competitorRepository.save(gabriel);

        Competitor ana = new Competitor();
        ana.setNome("Ana Teste");
        ana.setEmail("ana@ifba.rascomp.dev");
        ana.setTelefone("71999990002");
        ana.setTeam(equipeIfba);
        ana.setAtivo(true);
        ana = competitorRepository.save(ana);

        // Robôs de Seguidor de Linha
        Robot vespa = new Robot();
        vespa.setNome("Vespa");
        vespa.setDescricao("Robô seguidor de linha da RAS UFRB.");
        vespa.setTeam(rasUfrb);
        vespa.setAtivo(true);
        vespa = robotRepository.save(vespa);

        Robot trilho = new Robot();
        trilho.setNome("Trilho");
        trilho.setDescricao("Robô seguidor de linha da equipe de teste do IFBA.");
        trilho.setTeam(equipeIfba);
        trilho.setAtivo(true);
        trilho = robotRepository.save(trilho);

        // Competição com inscrições abertas na data atual de desenvolvimento
        Competition rrc2026 = new Competition();
        rrc2026.setNome("RRC 2026");
        rrc2026.setDescricao("Cenário de referência para testes integrados do Rascomp.");
        rrc2026.setInicioInscricoes(LocalDate.of(2026, 8, 1));
        rrc2026.setFimInscricoes(LocalDate.of(2026, 8, 31));
        rrc2026.setDataInicio(LocalDate.of(2026, 9, 5));
        rrc2026.setDataFim(LocalDate.of(2026, 9, 6));
        rrc2026.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        rrc2026.setAtivo(true);
        rrc2026 = competitionRepository.save(rrc2026);

        // Inscrições Follow: a modalidade é definida por ranking, sem Bracket/Match.
        Registration inscricaoVespa = new Registration();
        inscricaoVespa.setCompetition(rrc2026);
        inscricaoVespa.setCategory(seguidorLinha);
        inscricaoVespa.setTeam(rasUfrb);
        inscricaoVespa.setRobot(vespa);
        inscricaoVespa.setStatus(StatusRegistration.APROVADA);
        inscricaoVespa.setObservacao("Inscrição de teste da Vespa.");
        inscricaoVespa.setAtivo(true);
        inscricaoVespa = registrationRepository.save(inscricaoVespa);

        Registration inscricaoTrilho = new Registration();
        inscricaoTrilho.setCompetition(rrc2026);
        inscricaoTrilho.setCategory(seguidorLinha);
        inscricaoTrilho.setTeam(equipeIfba);
        inscricaoTrilho.setRobot(trilho);
        inscricaoTrilho.setStatus(StatusRegistration.APROVADA);
        inscricaoTrilho.setObservacao("Inscrição de teste do Trilho.");
        inscricaoTrilho.setAtivo(true);
        inscricaoTrilho = registrationRepository.save(inscricaoTrilho);

        // Tentativas de Seguidor de Linha usadas no ranking.
        TentativaSeguidorLinha tentativa1 = new TentativaSeguidorLinha();
        tentativa1.setRegistration(inscricaoVespa);
        tentativa1.setTomada(1);
        tentativa1.setNumeroTentativa(1);
        tentativa1.setTempoSegundos(new BigDecimal("42.315"));
        tentativa1.setCheckpointsAlcancados(5);
        tentativa1.setPenalidadeSegundos(0);
        tentativa1.setConcluida(true);
        tentativa1.setValida(true);
        tentativa1.setObservacao("Tentativa válida de referência.");
        tentativaSeguidorLinhaRepository.save(tentativa1);

        TentativaSeguidorLinha tentativa2 = new TentativaSeguidorLinha();
        tentativa2.setRegistration(inscricaoVespa);
        tentativa2.setTomada(1);
        tentativa2.setNumeroTentativa(2);
        tentativa2.setTempoSegundos(new BigDecimal("40.870"));
        tentativa2.setCheckpointsAlcancados(5);
        tentativa2.setPenalidadeSegundos(2);
        tentativa2.setConcluida(true);
        tentativa2.setValida(true);
        tentativa2.setObservacao("Segunda tentativa válida de referência.");
        tentativaSeguidorLinhaRepository.save(tentativa2);

        System.out.println("Dados de teste criados com sucesso.");
        System.out.println("IDs principais para testes no Postman:");
        System.out.println("- Categoria Seguidor de Linha: " + seguidorLinha.getId());
        System.out.println("- Instituição UFRB: " + ufrb.getId());
        System.out.println("- Instituição IFBA: " + ifba.getId());
        System.out.println("- Equipe RAS UFRB: " + rasUfrb.getId());
        System.out.println("- Equipe Robótica IFBA: " + equipeIfba.getId());
        System.out.println("- Competidor Gabriel: " + gabriel.getId());
        System.out.println("- Competidor Ana: " + ana.getId());
        System.out.println("- Robô Vespa: " + vespa.getId());
        System.out.println("- Robô Trilho: " + trilho.getId());
        System.out.println("- Competição RRC 2026: " + rrc2026.getId());
        System.out.println("- Inscrição Vespa: " + inscricaoVespa.getId());
        System.out.println("- Inscrição Trilho: " + inscricaoTrilho.getId());
        System.out.println("- Follow usa TentativaSeguidorLinha + ranking; não cria Bracket/Match/MatchResult.");
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
