package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "rascomp.test-data.bracket-history-enabled",
        havingValue = "true")
public class BracketHistoryTestDataInitializer implements CommandLineRunner {

    private static final String COMPETITION_NAME = "RRC 2026 - Teste Historico de Chaves";
    private static final String CATEGORY_NAME = "Mini Sumo - Teste Historico";
    private static final String INSTITUTION_SIGLA = "RRC-TESTE";

    private final CompetitionCategoryRepository competitionCategoryRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final CompetitionRepository competitionRepository;
    private final RegistrationRepository registrationRepository;
    private final InspecaoSumoService inspecaoSumoService;

    @Override
    @Transactional
    public void run(String... args) {
        CompetitionCategory category = garantirCategoria();
        garantirConfigSumo(category);

        Institution institution = garantirInstituicao();
        Competition competition = garantirCompeticao();

        Registration alfa = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE HC - Alfa", institution),
                "Atlas HC",
                new BigDecimal("0.432"));

        Registration beta = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE HC - Beta", institution),
                "Boreal HC",
                new BigDecimal("0.447"));

        Registration gama = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE HC - Gama", institution),
                "Cobalto HC",
                new BigDecimal("0.461"));

        Registration delta = garantirParticipante(
                competition,
                category,
                garantirEquipe("TESTE HC - Delta", institution),
                "Dardo HC",
                new BigDecimal("0.489"));

        System.out.println("============================================================");
        System.out.println("Cenario de teste do Historico de Chaves pronto.");
        System.out.println("Competicao: " + competition.getNome() + " (#" + competition.getId() + ")");
        System.out.println("Categoria: " + category.getNome() + " (#" + category.getId() + ")");
        System.out.println("Inscricoes aprovadas e aptas: "
                + alfa.getId() + ", " + beta.getId() + ", " + gama.getId() + ", " + delta.getId());
        System.out.println("Nenhuma chave foi gerada pelo seed: gere duas pela interface para validar o historico.");
        System.out.println("============================================================");
    }

    private CompetitionCategory garantirCategoria() {
        return competitionCategoryRepository.findAll().stream()
                .filter(item -> CATEGORY_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .map(item -> {
                    item.setModalidade(Modalidade.SUMO);
                    item.setAtivo(true);
                    return competitionCategoryRepository.save(item);
                })
                .orElseGet(() -> competitionCategoryRepository.save(
                        CompetitionCategory.builder()
                                .nome(CATEGORY_NAME)
                                .descricao("Categoria temporaria para validar geracao, regeneracao e historico de chaves.")
                                .modalidade(Modalidade.SUMO)
                                .ativo(true)
                                .build()));
    }

    private void garantirConfigSumo(CompetitionCategory category) {
        ConfigSumo config = configSumoRepository.findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigSumo.builder()
                        .competitionCategory(category)
                        .build());

        config.setPesoMax(new BigDecimal("0.500"));
        config.setExigeInspecao(true);
        config.setMaxTentativasInspecao(3);
        config.setNumeroRounds(3);
        config.setRoundsParaVencer(2);
        config.setPermiteRoundDesempate(true);
        configSumoRepository.save(config);
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
                    institution.setNome("Instituicao Temporaria de Teste RRC");
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
                    return competitionRepository.save(item);
                })
                .orElseGet(() -> {
                    Competition competition = new Competition();
                    competition.setNome(COMPETITION_NAME);
                    competition.setDescricao("Cenario temporario para validar o historico de chaveamentos do Sumô.");
                    competition.setInicioInscricoes(LocalDate.of(2026, 8, 1));
                    competition.setFimInscricoes(LocalDate.of(2026, 8, 31));
                    competition.setDataInicio(LocalDate.of(2026, 9, 5));
                    competition.setDataFim(LocalDate.of(2026, 9, 6));
                    competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
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
            String robotNome,
            BigDecimal pesoMedido) {

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
                    nova.setObservacao("Inscricao temporaria para teste do historico de chaves.");
                    return nova;
                });

        registration.setTeam(team);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);
        registration = registrationRepository.save(registration);

        if (!inspecaoSumoService.estaAptaParaCompetir(registration.getId())) {
            InspecaoSumoDTO dto = new InspecaoSumoDTO();
            dto.setRegistrationId(registration.getId());
            dto.setPesoMedido(pesoMedido);
            dto.setObservacao("Inspecao aprovada gerada pelo cenario temporario de teste.");
            inspecaoSumoService.registrar(dto);
        }

        return registration;
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
                    robot.setDescricao("Robo temporario para teste do historico de chaves.");
                    robot.setTeam(team);
                    robot.setAtivo(true);
                    return robotRepository.save(robot);
                });
    }
}
