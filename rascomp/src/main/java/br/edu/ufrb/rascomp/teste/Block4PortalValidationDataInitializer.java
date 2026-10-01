package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.SumoControlMode;
import br.edu.ufrb.rascomp.model.Enum.SumoPhysicalClass;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Cenário enxuto e idempotente para validar ETAPA 4 / BLOCO 4.3.
 *
 * Mantém uma competição com inscrições abertas e uma equipe com:
 * - líder PARTICIPANTE;
 * - membro PARTICIPANTE responsável apenas por um dos robôs;
 * - competidor de apoio sem conta;
 * - categorias Follow, Mini Sumô e Sumô 3 kg.
 *
 * Não cria Registration pronta: a inscrição deve nascer pelo Portal durante o teste.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "rascomp.test-data.block4-portal-validation-enabled",
        havingValue = "true")
public class Block4PortalValidationDataInitializer implements CommandLineRunner {

    public static final String DEV_EMAIL = "dev.b4@rascomp.local";
    public static final String GESTAO_EMAIL = "gestao.b4@rascomp.local";
    public static final String LEADER_EMAIL = "lider.b4@rascomp.local";
    public static final String MEMBER_EMAIL = "membro.b4@rascomp.local";
    public static final String PASSWORD = "Rascomp@2026";

    private static final String COMPETITION_NAME = "ETAPA 4 · BLOCO 4.3 · INSCRIÇÕES";
    private static final String TEAM_NAME = "B4 · Equipe Portal";
    private static final String INSTITUTION_SIGLA = "B4-QA";
    private static final String FOLLOW_CATEGORY = "B4 · Follow Line";
    private static final String MINI_CATEGORY = "B4 · Mini Sumô RC";
    private static final String SUMO_3KG_CATEGORY = "B4 · Sumô 3 kg RC";
    private static final String RESPONSIBLE_ROBOT = "B4 · Vespa";
    private static final String LEADER_ONLY_ROBOT = "B4 · Atlas";

    private final UserAccountRepository userAccountRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotResponsibleRepository robotResponsibleRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        garantirUsuario(DEV_EMAIL, "DEV B4", UserRole.DEV);
        garantirUsuario(GESTAO_EMAIL, "Gestão B4", UserRole.GESTAO);
        UserAccount liderUser = garantirUsuario(LEADER_EMAIL, "Líder B4", UserRole.PARTICIPANTE);
        UserAccount membroUser = garantirUsuario(MEMBER_EMAIL, "Membro B4", UserRole.PARTICIPANTE);

        Institution institution = garantirInstituicao();
        Team team = garantirEquipe(institution, liderUser);

        Competitor lider = garantirCompetidor("Líder B4", LEADER_EMAIL, team, liderUser);
        Competitor membro = garantirCompetidor("Membro B4", MEMBER_EMAIL, team, membroUser);
        Competitor apoio = garantirCompetidor("Apoio B4", "apoio.b4@rascomp.local", team, null);

        Robot vespa = garantirRobo(
                RESPONSIBLE_ROBOT,
                "Robô do membro para validar inscrição normal pelo Portal.",
                team);
        Robot atlas = garantirRobo(
                LEADER_ONLY_ROBOT,
                "Robô visível ao líder, mas sem responsabilidade permanente do membro.",
                team);

        garantirResponsabilidade(vespa, membro, membroUser);
        garantirResponsabilidade(vespa, apoio, liderUser);
        garantirResponsabilidade(atlas, apoio, liderUser);

        CompetitionCategory follow = garantirCategoria(
                FOLLOW_CATEGORY,
                Modalidade.FOLLOW_LINE,
                null,
                null);
        CompetitionCategory mini = garantirCategoria(
                MINI_CATEGORY,
                Modalidade.SUMO,
                SumoPhysicalClass.MINI_500G,
                SumoControlMode.RC);
        CompetitionCategory sumo3kg = garantirCategoria(
                SUMO_3KG_CATEGORY,
                Modalidade.SUMO,
                SumoPhysicalClass.SUMO_3KG,
                SumoControlMode.RC);

        garantirConfigFollow(follow);
        garantirConfigSumo(mini, new BigDecimal("0.500"));
        garantirConfigSumo(sumo3kg, new BigDecimal("3.000"));
        garantirCompeticaoAberta();

        System.out.println("============================================================");
        System.out.println("RASCOMP · ETAPA 4 / BLOCO 4.3 · CENARIO PORTAL PRONTO");
        System.out.println("Competição aberta: " + COMPETITION_NAME);
        System.out.println("Equipe: " + TEAM_NAME);
        System.out.println("DEV: " + DEV_EMAIL + " / " + PASSWORD);
        System.out.println("GESTAO: " + GESTAO_EMAIL + " / " + PASSWORD);
        System.out.println("LIDER: " + LEADER_EMAIL + " / " + PASSWORD);
        System.out.println("MEMBRO: " + MEMBER_EMAIL + " / " + PASSWORD);
        System.out.println("Membro responsável por: " + RESPONSIBLE_ROBOT);
        System.out.println("Robô exclusivo da visão administrativa do líder: " + LEADER_ONLY_ROBOT);
        System.out.println("Nenhuma Registration é pré-criada: o teste começa no Portal.");
        System.out.println("============================================================");
    }

    private UserAccount garantirUsuario(String email, String nome, UserRole role) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseGet(UserAccount::new);
        user.setNome(nome);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setAtivo(true);
        if (user.getSessionVersion() == null) user.setSessionVersion(0L);
        return userAccountRepository.save(user);
    }

    private Institution garantirInstituicao() {
        Institution institution = institutionRepository.findAll().stream()
                .filter(item -> INSTITUTION_SIGLA.equalsIgnoreCase(item.getSigla()))
                .findFirst()
                .orElseGet(Institution::new);
        institution.setNome("Instituição QA Portal");
        institution.setSigla(INSTITUTION_SIGLA);
        institution.setCidade("Cruz das Almas");
        institution.setEstado("BA");
        institution.setAtivo(true);
        return institutionRepository.save(institution);
    }

    private Team garantirEquipe(Institution institution, UserAccount lider) {
        Team team = teamRepository.findAll().stream()
                .filter(item -> TEAM_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Team::new);
        team.setNome(TEAM_NAME);
        team.setInstitution(institution);
        team.setResponsibleUser(lider);
        team.setAtivo(true);
        return teamRepository.save(team);
    }

    private Competitor garantirCompetidor(
            String nome,
            String email,
            Team team,
            UserAccount userAccount) {

        Competitor competitor = competitorRepository.findByEmailIgnoreCase(email)
                .orElseGet(Competitor::new);
        competitor.setNome(nome);
        competitor.setEmail(email);
        competitor.setTelefone("75999990000");
        competitor.setTeam(team);
        competitor.setUserAccount(userAccount);
        competitor.setAtivo(true);
        return competitorRepository.save(competitor);
    }

    private Robot garantirRobo(String nome, String descricao, Team team) {
        Robot robot = robotRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .filter(item -> item.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(Robot::new);
        robot.setNome(nome);
        robot.setDescricao(descricao);
        robot.setTeam(team);
        robot.setAtivo(true);
        return robotRepository.save(robot);
    }

    private void garantirResponsabilidade(
            Robot robot,
            Competitor competitor,
            UserAccount createdBy) {

        RobotResponsible link = robotResponsibleRepository
                .findByRobotIdAndCompetitorId(robot.getId(), competitor.getId())
                .orElseGet(RobotResponsible::new);
        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setCreatedByUser(createdBy);
        link.setAtivo(true);
        robotResponsibleRepository.save(link);
    }

    private CompetitionCategory garantirCategoria(
            String nome,
            Modalidade modalidade,
            SumoPhysicalClass physicalClass,
            SumoControlMode controlMode) {

        CompetitionCategory category = categoryRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(CompetitionCategory::new);
        category.setNome(nome);
        category.setDescricao("Categoria de QA do fluxo normal de inscrição do Portal.");
        category.setModalidade(modalidade);
        category.setSumoPhysicalClass(physicalClass);
        category.setSumoControlMode(controlMode);
        category.setAtivo(true);
        return categoryRepository.save(category);
    }

    private void garantirConfigFollow(CompetitionCategory category) {
        ConfigFollow config = configFollowRepository
                .findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigFollow.builder()
                        .competitionCategory(category)
                        .build());
        config.setNumeroTomadas(3);
        config.setTentativasPorTomada(3);
        config.setMaxTempoSegundos(120);
        config.setNumeroCheckpoints(5);
        config.setPenalidadePadraoSegundos(10);
        config.setTempoApresentacaoSegundos(60);
        configFollowRepository.save(config);
    }

    private void garantirConfigSumo(CompetitionCategory category, BigDecimal pesoMax) {
        ConfigSumo config = configSumoRepository
                .findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigSumo.builder()
                        .competitionCategory(category)
                        .build());
        config.setPesoMax(pesoMax);
        config.setExigeInspecao(true);
        config.setMaxTentativasInspecao(3);
        config.setNumeroRounds(3);
        config.setRoundsParaVencer(2);
        config.setPermiteRoundDesempate(true);
        config.setMaxRoundsExtras(2);
        configSumoRepository.save(config);
    }

    private Competition garantirCompeticaoAberta() {
        LocalDate hoje = LocalDate.now();
        Competition competition = competitionRepository.findAll().stream()
                .filter(item -> COMPETITION_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Competition::new);
        competition.setNome(COMPETITION_NAME);
        competition.setDescricao("Competição aberta exclusivamente para validar o BLOCO 4.3.");
        competition.setInicioInscricoes(hoje.minusDays(7));
        competition.setFimInscricoes(hoje.plusDays(7));
        competition.setDataInicio(hoje.plusDays(14));
        competition.setDataFim(hoje.plusDays(16));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competitionRepository.limparVigente();
        competition.setVigente(true);
        competition.setAtivo(true);
        return competitionRepository.save(competition);
    }
}
