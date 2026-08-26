package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
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
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "rascomp.test-data.follow-line-enabled",
        havingValue = "true")
public class FollowLineTestDataInitializer implements CommandLineRunner {

    private static final String COMPETITION_NAME = "RRC 2026 - Teste Follow Line";
    private static final String CATEGORY_NAME = "Seguidor de Linha - Teste Operacao";
    private static final String INSTITUTION_SIGLA = "RRC-FOLLOW";

    private final CompetitionCategoryRepository competitionCategoryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final CompetitionRepository competitionRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        CompetitionCategory category = garantirCategoria();
        garantirConfig(category);
        Institution institution = garantirInstituicao();
        Competition competition = garantirCompeticao();

        Registration veloz = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE FL - Veloz", institution),
                "Veloz FL");
        Registration falcao = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE FL - Falcao", institution),
                "Falcao FL");
        Registration trilha = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE FL - Trilha", institution),
                "Trilha FL");
        Registration pulso = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE FL - Pulso", institution),
                "Pulso FL");

        // Veloz: tomada 1 representada por 42.315 s; tomada 2 e melhor, com 41.100 s.
        garantirTentativa(veloz, 1, 1, "42.315", 5, 0, true, true,
                "Tomada 1 - tentativa valida de referencia.");
        garantirTentativa(veloz, 1, 2, "40.870", 5, 2, true, true,
                "Tomada 1 - tentativa mais rapida, mas com penalidade.");
        garantirTentativa(veloz, 2, 1, "41.100", 5, 0, true, true,
                "Tomada 2 - melhor tomada do Veloz no cenario.");

        // Falcao: tomada 1 permanece a melhor; tomada 2 serve para validar historico agrupado.
        garantirTentativa(falcao, 1, 1, "39.500", 5, 0, true, true,
                "Tomada 1 - melhor tempo inicial do cenario.");
        garantirTentativa(falcao, 2, 1, "40.200", 5, 0, true, true,
                "Tomada 2 - valida, mas pior que a tomada 1.");

        garantirTentativa(trilha, 1, 1, "125.000", 4, 0, true, false,
                "Tomada 1 - tentativa acima do limite configurado e marcada como invalida.");
        garantirTentativa(pulso, 1, 1, "65.200", 3, 0, false, true,
                "Tomada 1 - tentativa interrompida para validar historico de nao concluidas.");

        System.out.println("============================================================");
        System.out.println("Cenario de teste do Follow Line pronto.");
        System.out.println("Competicao: " + competition.getNome() + " (#" + competition.getId() + ")");
        System.out.println("Categoria: " + category.getNome() + " (#" + category.getId() + ")");
        System.out.println("Robos: Veloz FL, Falcao FL, Trilha FL e Pulso FL.");
        System.out.println("Ha tomadas com multiplas tentativas, duas tomadas no mesmo robo, invalida e nao concluida.");
        System.out.println("Ainda existem slots livres para novos registros pela tela operacional.");
        System.out.println("============================================================");
    }

    private CompetitionCategory garantirCategoria() {
        return competitionCategoryRepository.findAll().stream()
                .filter(item -> CATEGORY_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .map(item -> {
                    item.setModalidade(Modalidade.FOLLOW_LINE);
                    item.setAtivo(true);
                    return competitionCategoryRepository.save(item);
                })
                .orElseGet(() -> competitionCategoryRepository.save(
                        CompetitionCategory.builder()
                                .nome(CATEGORY_NAME)
                                .descricao("Categoria temporaria para validar a operacao administrativa do Follow Line.")
                                .modalidade(Modalidade.FOLLOW_LINE)
                                .ativo(true)
                                .build()));
    }

    private void garantirConfig(CompetitionCategory category) {
        ConfigFollow config = configFollowRepository.findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigFollow.builder()
                        .competitionCategory(category)
                        .build());

        config.setNumeroTomadas(2);
        config.setTentativasPorTomada(3);
        config.setMaxTempoSegundos(120);
        config.setNumeroCheckpoints(5);
        configFollowRepository.save(config);
    }

    private Institution garantirInstituicao() {
        return institutionRepository.findAll().stream()
                .filter(item -> INSTITUTION_SIGLA.equalsIgnoreCase(item.getSigla()))
                .findFirst()
                .map(item -> {
                    item.setAtivo(true);
                    return institutionRepository.save(item);
                })
                .orElseGet(() -> {
                    Institution institution = new Institution();
                    institution.setNome("Instituicao Temporaria de Teste Follow Line");
                    institution.setSigla(INSTITUTION_SIGLA);
                    institution.setCidade("Cruz das Almas");
                    institution.setEstado("BA");
                    institution.setAtivo(true);
                    return institutionRepository.save(institution);
                });
    }

    private Competition garantirCompeticao() {
        return competitionRepository.findAll().stream()
                .filter(item -> COMPETITION_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .map(item -> {
                    item.setAtivo(true);
                    item.setStatus(StatusCompetition.EM_ANDAMENTO);
                    return competitionRepository.save(item);
                })
                .orElseGet(() -> {
                    Competition competition = new Competition();
                    competition.setNome(COMPETITION_NAME);
                    competition.setDescricao("Cenario temporario para validar tomadas, tentativas, historico e ranking do Follow Line.");
                    competition.setInicioInscricoes(LocalDate.of(2026, 8, 1));
                    competition.setFimInscricoes(LocalDate.of(2026, 8, 24));
                    competition.setDataInicio(LocalDate.of(2026, 8, 25));
                    competition.setDataFim(LocalDate.of(2026, 8, 26));
                    competition.setStatus(StatusCompetition.EM_ANDAMENTO);
                    competition.setAtivo(true);
                    return competitionRepository.save(competition);
                });
    }

    private Team garantirEquipe(String nome, Institution institution) {
        return teamRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .map(item -> {
                    item.setInstitution(institution);
                    item.setAtivo(true);
                    return teamRepository.save(item);
                })
                .orElseGet(() -> {
                    Team team = new Team();
                    team.setNome(nome);
                    team.setInstitution(institution);
                    team.setAtivo(true);
                    return teamRepository.save(team);
                });
    }

    private Registration garantirParticipante(
            Competition competition,
            CompetitionCategory category,
            Team team,
            String robotNome) {

        Robot robot = garantirRobo(robotNome, team);

        Registration registration = registrationRepository.findAll().stream()
                .filter(item -> item.getCompetition().getId().equals(competition.getId()))
                .filter(item -> item.getCategory().getId().equals(category.getId()))
                .filter(item -> item.getRobot().getId().equals(robot.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Registration nova = new Registration();
                    nova.setCompetition(competition);
                    nova.setCategory(category);
                    nova.setTeam(team);
                    nova.setRobot(robot);
                    nova.setObservacao("Inscricao temporaria para teste do Follow Line.");
                    return nova;
                });

        registration.setTeam(team);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);
        return registrationRepository.save(registration);
    }

    private Robot garantirRobo(String nome, Team team) {
        return robotRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .filter(item -> item.getTeam().getId().equals(team.getId()))
                .findFirst()
                .map(item -> {
                    item.setAtivo(true);
                    return robotRepository.save(item);
                })
                .orElseGet(() -> {
                    Robot robot = new Robot();
                    robot.setNome(nome);
                    robot.setDescricao("Robo temporario para teste da operacao Follow Line.");
                    robot.setTeam(team);
                    robot.setAtivo(true);
                    return robotRepository.save(robot);
                });
    }

    private void garantirTentativa(
            Registration registration,
            int tomada,
            int numeroTentativa,
            String tempo,
            int checkpoints,
            int penalidade,
            boolean concluida,
            boolean valida,
            String observacao) {

        if (tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(
                registration.getId(),
                tomada,
                numeroTentativa)) {
            return;
        }

        TentativaSeguidorLinha tentativa = new TentativaSeguidorLinha();
        tentativa.setRegistration(registration);
        tentativa.setTomada(tomada);
        tentativa.setNumeroTentativa(numeroTentativa);
        tentativa.setTempoSegundos(new BigDecimal(tempo));
        tentativa.setCheckpointsAlcancados(checkpoints);
        tentativa.setPenalidadeSegundos(penalidade);
        tentativa.setConcluida(concluida);
        tentativa.setValida(valida);
        tentativa.setObservacao(observacao);
        tentativaRepository.save(tentativa);
    }
}
