package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.*;
import br.edu.ufrb.rascomp.model.Enum.*;
import br.edu.ufrb.rascomp.repository.*;
import lombok.RequiredArgsConstructor;

/** Dados inteiramente ficticios para a operacao de homologacao RASCOMP 2026. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rascomp.test-data.homologacao-2026-enabled", havingValue = "true")
public class Homologacao2026DataInitializer implements CommandLineRunner {
    private static final String NAME = "H26 | RASCOMP - SIMULACAO OPERACIONAL";
    public static final String DEV_EMAIL = "dev.h26@rascomp.local";
    public static final String GESTAO_EMAIL = "gestao.h26@rascomp.local";
    public static final String PASSWORD = "Rascomp@2026";
    private final UserAccountRepository userAccounts;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final RobotResponsibleRepository robotResponsibles;
    private final CompetitionRepository competitions;
    private final CompetitionCategoryRepository categories;
    private final InstitutionRepository institutions;
    private final TeamRepository teams;
    private final CompetitorRepository competitors;
    private final RobotRepository robots;
    private final RegistrationRepository registrations;
    private final ConfigSumoRepository sumoConfigs;
    private final ConfigFollowRepository followConfigs;
    private final CompetitionJudgeRepository judges;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> profiles = List.of(environment.getActiveProfiles());
        if (!profiles.contains("testdata") || profiles.contains("cloud"))
            throw new IllegalStateException("H26 requer somente perfil testdata, sem cloud");
        String url = environment.getProperty("spring.datasource.url", "").toLowerCase();
        if (!(url.contains("localhost") || url.contains("127.0.0.1"))
                || !(url.contains("/rascomp_b4_validation") || url.contains("/rascomp_homologacao_2026")))
            throw new IllegalStateException("H26 requer MySQL local e banco exclusivo de homologacao");
        // As contas de homologacao sao garantidas mesmo quando a competicao ja existe.
        garantirUsuarioInterno(DEV_EMAIL, "DEV Homologacao 2026", UserRole.DEV);
        garantirUsuarioInterno(GESTAO_EMAIL, "Gestao Homologacao 2026", UserRole.GESTAO);
        if (competitions.findAll().stream().anyMatch(c -> NAME.equals(c.getNome()))) {
            System.out.println("[H26] Cenario existente; contas DEV/GESTAO conferidas.");
            imprimirAcessoHomologacao();
            return;
        }

        Competition competition = new Competition();
        competition.setNome(NAME);
        competition.setDescricao("Competicao ficticia: inspecao, agenda, chaveamento e resultados.");
        competition.setInicioInscricoes(LocalDate.of(2026, 10, 1));
        competition.setFimInscricoes(LocalDate.of(2026, 11, 12));
        competition.setDataInicio(LocalDate.of(2026, 11, 14));
        competition.setDataFim(LocalDate.of(2026, 11, 14));
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setAtivo(true);
        competitions.limparVigente();
        competition.setVigente(true);
        competition = competitions.save(competition);

        List<Institution> inst = new ArrayList<>();
        String[] names = {"Instituto Aurora", "Universidade Horizonte", "Centro Tecnico Sertao", "Faculdade Nova Era"};
        for (int i = 0; i < names.length; i++) {
            Institution item = new Institution();
            item.setNome("H26 | " + names[i]);
            item.setSigla("H26-" + (i + 1));
            item.setCidade("Cruz das Almas");
            item.setEstado("BA");
            item.setAtivo(true);
            inst.add(institutions.save(item));
        }

        List<Team> teamList = new ArrayList<>();
        List<List<Competitor>> members = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Team team = new Team();
            team.setNome(String.format("H26 | Equipe %02d", i + 1));
            team.setInstitution(inst.get(i % 4));
            team.setAtivo(true);
            team = teams.save(team);
            teamList.add(team);
            List<Competitor> group = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Competitor person = new Competitor();
                person.setNome(String.format("H26 Competidor %02d-%02d", i + 1, j + 1));
                person.setEmail(String.format("h26.e%02d.p%02d@example.invalid", i + 1, j + 1));
                person.setTeam(team);
                person.setAtivo(true);
                group.add(competitors.save(person));
            }
            members.add(group);
        }

        CompetitionCategory[] cats = {
            sumo("H26 | Mini Sumo 13", SumoPhysicalClass.MINI_500G, new BigDecimal("0.500")),
            sumo("H26 | Mini Sumo 15", SumoPhysicalClass.MINI_500G, new BigDecimal("0.500")),
            sumo("H26 | Sumo 3kg 20", SumoPhysicalClass.SUMO_3KG, new BigDecimal("3.000")),
            follow("H26 | Follow 4"), follow("H26 | Follow 6")
        };
        for (CompetitionCategory category : cats) {
            jdbcTemplate.update(
                    "INSERT INTO competition_enabled_categories (competition_id, category_id) VALUES (?, ?)",
                    competition.getId(), category.getId());
        }
        int[] sizes = {13, 15, 20, 4, 6};
        int index = 0;
        for (int k = 0; k < cats.length; k++) {
            for (int j = 0; j < sizes[k]; j++) {
                int teamIndex = index % teamList.size();
                Team team = teamList.get(teamIndex);
                Robot robot = new Robot();
                robot.setNome(String.format("H26-R%03d", index + 1));
                robot.setDescricao("Robo ficticio de homologacao.");
                robot.setTeam(team);
                robot.setAtivo(true);
                robot = robots.save(robot);

                for (Competitor responsible : members.get(teamIndex).subList(0, 2)) {
                    RobotResponsible link = new RobotResponsible();
                    link.setRobot(robot);
                    link.setCompetitor(responsible);
                    link.setAtivo(true);
                    robotResponsibles.save(link);
                }

                Registration registration = new Registration();
                registration.setCompetition(competition);
                registration.setCategory(cats[k]);
                registration.setTeam(team);
                registration.setRobot(robot);
                registration.setStatus(StatusRegistration.APROVADA);
                registration.setReviewedAt(LocalDateTime.now());
                registration.setAtivo(true);
                registration.setCompetitors(new LinkedHashSet<>(members.get(teamIndex).subList(0, 2)));
                registrations.save(registration);
                index++;
            }
        }
        for (int i = 1; i <= 3; i++) {
            CompetitionJudge judge = new CompetitionJudge();
            judge.setCompetition(competition);
            judge.setNome("H26 | Arbitro " + i);
            judge.setAtivo(true);
            judges.save(judge);
        }
        System.out.println("[H26] 1 competicao, 4 instituicoes, 12 equipes, 60 competidores, 58 robos/inscricoes, 3 arbitros.");
        System.out.println("[H26] Inspecoes, chaves, agenda e resultados permanecem livres para teste manual.");
        imprimirAcessoHomologacao();
    }

    private void garantirUsuarioInterno(String email, String nome, UserRole role) {
        UserAccount user = userAccounts.findByEmailIgnoreCase(email).orElseGet(UserAccount::new);
        if (user.getId() != null && user.getRole() != role) {
            throw new IllegalStateException("[H26] Conta reservada de homologacao ja existe com papel diferente: " + email);
        }
        user.setNome(nome);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setAtivo(true);
        user.setEmailVerificadoEm(LocalDateTime.now());
        if (user.getSessionVersion() == null) user.setSessionVersion(0L);
        userAccounts.save(user);
    }

    private void imprimirAcessoHomologacao() {
        System.out.println("[H26] DEV: " + DEV_EMAIL + " / " + PASSWORD);
        System.out.println("[H26] GESTAO: " + GESTAO_EMAIL + " / " + PASSWORD);
    }

    private CompetitionCategory sumo(String name, SumoPhysicalClass physical, BigDecimal weight) {
        CompetitionCategory cat = new CompetitionCategory();
        cat.setNome(name);
        cat.setDescricao("Categoria ficticia de homologacao");
        cat.setModalidade(Modalidade.SUMO);
        cat.setSumoPhysicalClass(physical);
        cat.setSumoControlMode(SumoControlMode.RC);
        cat.setAtivo(true);
        cat = categories.save(cat);
        ConfigSumo config = new ConfigSumo();
        config.setCompetitionCategory(cat);
        config.setPesoMax(weight);
        config.setExigeInspecao(true);
        config.setMaxTentativasInspecao(3);
        config.setNumeroRounds(3);
        config.setRoundsParaVencer(2);
        config.setPermiteRoundDesempate(true);
        config.setMaxRoundsExtras(2);
        sumoConfigs.save(config);
        return cat;
    }

    private CompetitionCategory follow(String name) {
        CompetitionCategory cat = new CompetitionCategory();
        cat.setNome(name);
        cat.setDescricao("Categoria ficticia de homologacao");
        cat.setModalidade(Modalidade.FOLLOW_LINE);
        cat.setAtivo(true);
        cat = categories.save(cat);
        ConfigFollow config = new ConfigFollow();
        config.setCompetitionCategory(cat);
        config.setNumeroTomadas(3);
        config.setTentativasPorTomada(3);
        config.setMaxTempoSegundos(120);
        config.setNumeroCheckpoints(5);
        config.setPenalidadePadraoSegundos(10);
        config.setTempoApresentacaoSegundos(60);
        followConfigs.save(config);
        return cat;
    }
}
