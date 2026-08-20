package br.edu.ufrb.rascomp.teste;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Cria cenários mínimos e idempotentes para a bateria manual do Postman.
 *
 * Ativação local:
 * RASCOMP_SEED_POSTMAN=true
 *
 * O componente fica desabilitado por padrão e não interfere no seed normal.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rascomp.seed.postman", havingValue = "true")
public class PostmanScenarioInitializer implements CommandLineRunner {

    private static final String FOLLOW_COMPETITION = "POSTMAN - Chaveamento Follow";
    private static final String SUMO_COMPETITION = "POSTMAN - Sumô";

    private final CompetitionCategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final CompetitionRepository competitionRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        CompetitionCategory follow = buscarCategoriaPreferencial("Seguidor de Linha", Modalidade.FOLLOW_LINE);
        CompetitionCategory sumo = buscarCategoriaPreferencial("Mini Sumô", Modalidade.SUMO);

        List<Team> equipesAtivas = teamRepository.findAll().stream()
                .filter(team -> Boolean.TRUE.equals(team.getAtivo()))
                .toList();

        if (equipesAtivas.isEmpty()) {
            throw new EntityNotFoundException("Nenhuma equipe ativa encontrada para criar os cenários do Postman.");
        }

        Team equipeA = buscarEquipePreferencial(equipesAtivas, "RAS UFRB", equipesAtivas.get(0));
        Team equipeB = buscarEquipePreferencial(
                equipesAtivas,
                "Robótica IFBA",
                equipesAtivas.size() > 1 ? equipesAtivas.get(1) : equipeA);

        Competition followCompetition = buscarOuCriarCompetition(
                FOLLOW_COMPETITION,
                "Cenário isolado com três inscrições aprovadas para validar chave de quatro posições e BYE.");

        Registration followA = criarInscricaoSeNecessario(
                followCompetition,
                follow,
                equipeA,
                buscarOuCriarRobot("Postman Follow A", equipeA));
        Registration followB = criarInscricaoSeNecessario(
                followCompetition,
                follow,
                equipeB,
                buscarOuCriarRobot("Postman Follow B", equipeB));
        Registration followC = criarInscricaoSeNecessario(
                followCompetition,
                follow,
                equipeA,
                buscarOuCriarRobot("Postman Follow C", equipeA));

        Competition sumoCompetition = buscarOuCriarCompetition(
                SUMO_COMPETITION,
                "Cenário isolado para inspeção, desclassificação, chaveamento e rounds de Sumô.");

        Registration sumoA = criarInscricaoSeNecessario(
                sumoCompetition,
                sumo,
                equipeA,
                buscarOuCriarRobot("Postman Sumo A", equipeA));
        Registration sumoB = criarInscricaoSeNecessario(
                sumoCompetition,
                sumo,
                equipeB,
                buscarOuCriarRobot("Postman Sumo B", equipeB));
        Registration sumoC = criarInscricaoSeNecessario(
                sumoCompetition,
                sumo,
                equipeA,
                buscarOuCriarRobot("Postman Sumo C", equipeA));

        System.out.println("============================================================");
        System.out.println("CENÁRIOS POSTMAN PRONTOS");
        System.out.println("FOLLOW - competição: " + followCompetition.getId()
                + " | categoria: " + follow.getId()
                + " | inscrições: " + followA.getId() + ", " + followB.getId() + ", " + followC.getId());
        System.out.println("SUMO   - competição: " + sumoCompetition.getId()
                + " | categoria: " + sumo.getId()
                + " | inscrições: " + sumoA.getId() + ", " + sumoB.getId() + ", " + sumoC.getId());
        System.out.println("Nenhum bracket ou inspeção é criado: esses registros ficam para os testes manuais.");
        System.out.println("============================================================");
    }

    private CompetitionCategory buscarCategoriaPreferencial(String nome, Modalidade modalidade) {
        return categoryRepository.findByModalidadeAndAtivoTrue(modalidade).stream()
                .filter(category -> nome.equals(category.getNome()))
                .findFirst()
                .or(() -> categoryRepository.findByModalidadeAndAtivoTrue(modalidade).stream().findFirst())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhuma categoria ativa encontrada para a modalidade " + modalidade + "."));
    }

    private Team buscarEquipePreferencial(List<Team> equipes, String nome, Team fallback) {
        return equipes.stream()
                .filter(team -> nome.equals(team.getNome()))
                .findFirst()
                .orElse(fallback);
    }

    private Competition buscarOuCriarCompetition(String nome, String descricao) {
        return competitionRepository.findAll().stream()
                .filter(competition -> nome.equals(competition.getNome()))
                .findFirst()
                .orElseGet(() -> {
                    Competition competition = new Competition();
                    competition.setNome(nome);
                    competition.setDescricao(descricao);
                    competition.setInicioInscricoes(LocalDate.of(2026, 8, 1));
                    competition.setFimInscricoes(LocalDate.of(2026, 8, 31));
                    competition.setDataInicio(LocalDate.of(2026, 9, 12));
                    competition.setDataFim(LocalDate.of(2026, 9, 13));
                    competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
                    competition.setAtivo(true);
                    return competitionRepository.save(competition);
                });
    }

    private Robot buscarOuCriarRobot(String nome, Team team) {
        return robotRepository.findAll().stream()
                .filter(robot -> nome.equals(robot.getNome()))
                .filter(robot -> robot.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Robot robot = new Robot();
                    robot.setNome(nome);
                    robot.setDescricao("Robô criado exclusivamente para a bateria manual do Postman.");
                    robot.setTeam(team);
                    robot.setAtivo(true);
                    return robotRepository.save(robot);
                });
    }

    private Registration criarInscricaoSeNecessario(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot) {

        return registrationRepository.findAll().stream()
                .filter(registration -> registration.getCompetition().getId().equals(competition.getId()))
                .filter(registration -> registration.getCategory().getId().equals(category.getId()))
                .filter(registration -> registration.getRobot().getId().equals(robot.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Registration registration = new Registration();
                    registration.setCompetition(competition);
                    registration.setCategory(category);
                    registration.setTeam(team);
                    registration.setRobot(robot);
                    registration.setStatus(StatusRegistration.APROVADA);
                    registration.setObservacao("Inscrição preparada automaticamente para testes no Postman.");
                    registration.setAtivo(true);
                    return registrationRepository.save(registration);
                });
    }
}
