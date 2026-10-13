package br.edu.ufrb.rascomp.teste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.CompetitionJudge;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.SumoControlMode;
import br.edu.ufrb.rascomp.model.Enum.SumoPhysicalClass;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionJudgeRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import lombok.RequiredArgsConstructor;

/**
 * Cenário único e enxuto para a validação manual da ETAPA 4 / BLOCO 3.
 *
 * O profile testdata usa um banco separado e habilita somente este initializer.
 * A intenção é evitar dezenas de competições/cadastros concorrentes durante o QA manual.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "rascomp.test-data.block3-validation-enabled",
        havingValue = "true")
public class Block3ValidationDataInitializer implements CommandLineRunner {

    public static final String DEV_EMAIL = "dev.b3@rascomp.local";
    public static final String GESTAO_EMAIL = "gestao.b3@rascomp.local";
    public static final String PASSWORD = "Rascomp@2026";

    private static final String COMPETITION_NAME = "ETAPA 4 · BLOCO 3 · VALIDAÇÃO";
    private static final String FOLLOW_MAIN = "B3 · Follow Operação";
    private static final String FOLLOW_EXCEPTION = "B3 · Follow Exceção";
    private static final String SUMO_MAIN = "B3 · Mini Sumô RC";
    private static final String INSTITUTION_SIGLA = "B3-QA";

    private final UserAccountRepository userAccountRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final CompetitionJudgeRepository judgeRepository;
    private final BracketRepository bracketRepository;

    private final PasswordEncoder passwordEncoder;
    private final InspecaoSumoService inspecaoSumoService;
    private final BracketGenerationService bracketGenerationService;

    @Override
    @Transactional
    public void run(String... args) {
        UserAccount dev = garantirUsuario(
                DEV_EMAIL,
                "DEV Bloco 3",
                UserRole.DEV);
        garantirUsuario(
                GESTAO_EMAIL,
                "Gestão Bloco 3",
                UserRole.GESTAO);

        autenticarDev(dev);

        Competition competition = garantirCompeticao();

        CompetitionCategory followMain = garantirFollowCategory(
                FOLLOW_MAIN,
                "Categoria limpa para operação manual de chamadas, fila e tentativas.");
        CompetitionCategory followException = garantirFollowCategory(
                FOLLOW_EXCEPTION,
                "Categoria pré-preparada para Tomada Extra e decisão da organização.");
        CompetitionCategory sumoMain = garantirSumoCategory();

        garantirConfigFollow(followMain);
        garantirConfigFollow(followException);
        garantirConfigSumo(sumoMain);

        Institution institution = garantirInstituicao();
        List<Team> teams = garantirEquipes(institution);

        criarFollowPrincipal(competition, followMain, teams);
        criarFollowExcecao(competition, followException, teams);
        criarSumo(competition, sumoMain, teams);

        garantirJuizes(competition);
        garantirDuasChavesDeSumo(competition, sumoMain);

        competition.setStatus(StatusCompetition.EM_ANDAMENTO);
        competition.setVigente(true);
        competitionRepository.save(competition);

        SecurityContextHolder.clearContext();

        System.out.println("============================================================");
        System.out.println("RASCOMP · ETAPA 4 / BLOCO 3 · CENÁRIO ÚNICO DE VALIDAÇÃO");
        System.out.println("Competição: " + competition.getNome() + " (#" + competition.getId() + ")");
        System.out.println("DEV: " + DEV_EMAIL + " / " + PASSWORD);
        System.out.println("GESTAO: " + GESTAO_EMAIL + " / " + PASSWORD);
        System.out.println("Follow principal: " + FOLLOW_MAIN + " — 4 robôs livres para operação.");
        System.out.println("Follow exceção: " + FOLLOW_EXCEPTION
                + " — programa normal já encerrado sem tentativa classificável.");
        System.out.println("Sumô: " + SUMO_MAIN
                + " — 5 inscritos aptos na chave atual + 2 sem inspeção para testes.");
        System.out.println("Há uma chave histórica e uma chave vigente do Sumô; a vigente contém BYE.");
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
        user.setEmailVerificadoEm(LocalDateTime.now());
        if (user.getSessionVersion() == null) user.setSessionVersion(0L);
        return userAccountRepository.save(user);
    }

    private void autenticarDev(UserAccount dev) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        dev,
                        dev.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_DEV"))));
    }

    private Competition garantirCompeticao() {
        Competition competition = competitionRepository.findAll().stream()
                .filter(item -> COMPETITION_NAME.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Competition::new);

        competition.setNome(COMPETITION_NAME);
        competition.setDescricao(
                "Cenário único para a bateria manual do BLOCO 3: Follow, Sumô, chaves, agenda e resultados.");
        competition.setInicioInscricoes(LocalDate.of(2026, 9, 1));
        competition.setFimInscricoes(LocalDate.of(2026, 9, 20));
        competition.setDataInicio(LocalDate.of(2026, 9, 29));
        competition.setDataFim(LocalDate.of(2026, 9, 30));
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setAtivo(true);

        competitionRepository.limparVigente();
        competition.setVigente(true);
        return competitionRepository.save(competition);
    }

    private CompetitionCategory garantirFollowCategory(String nome, String descricao) {
        CompetitionCategory category = categoryRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(CompetitionCategory::new);

        category.setNome(nome);
        category.setDescricao(descricao);
        category.setModalidade(Modalidade.FOLLOW_LINE);
        category.setSumoPhysicalClass(null);
        category.setSumoControlMode(null);
        category.setAtivo(true);
        return categoryRepository.save(category);
    }

    private CompetitionCategory garantirSumoCategory() {
        CompetitionCategory category = categoryRepository.findAll().stream()
                .filter(item -> SUMO_MAIN.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(CompetitionCategory::new);

        category.setNome(SUMO_MAIN);
        category.setDescricao(
                "Categoria única de Sumô para inspeção, BYE, rounds, progressão, correção e resultado.");
        category.setModalidade(Modalidade.SUMO);
        category.setSumoPhysicalClass(SumoPhysicalClass.MINI_500G);
        category.setSumoControlMode(SumoControlMode.RC);
        category.setAtivo(true);
        return categoryRepository.save(category);
    }

    private void garantirConfigFollow(CompetitionCategory category) {
        ConfigFollow config = configFollowRepository.findByCompetitionCategoryId(category.getId())
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
        config.setMaxRoundsExtras(2);
        configSumoRepository.save(config);
    }

    private Institution garantirInstituicao() {
        Institution institution = institutionRepository.findAll().stream()
                .filter(item -> INSTITUTION_SIGLA.equalsIgnoreCase(item.getSigla()))
                .findFirst()
                .orElseGet(Institution::new);

        institution.setNome("Instituição QA Bloco 3");
        institution.setSigla(INSTITUTION_SIGLA);
        institution.setCidade("Cruz das Almas");
        institution.setEstado("BA");
        institution.setAtivo(true);
        return institutionRepository.save(institution);
    }

    private List<Team> garantirEquipes(Institution institution) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String nome = "B3 · Equipe " + i;
            Team team = teamRepository.findAll().stream()
                    .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                    .findFirst()
                    .orElseGet(Team::new);
            team.setNome(nome);
            team.setInstitution(institution);
            team.setAtivo(true);
            teams.add(teamRepository.save(team));
        }
        return teams;
    }

    private void criarFollowPrincipal(
            Competition competition,
            CompetitionCategory category,
            List<Team> teams) {

        for (int i = 1; i <= 4; i++) {
            Team team = teams.get(i - 1);
            Robot robot = garantirRobo(
                    String.format("F-%02d", i),
                    "Follow principal — livre para os testes manuais.",
                    team);
            garantirInscricao(competition, category, team, robot);
        }
    }

    private void criarFollowExcecao(
            Competition competition,
            CompetitionCategory category,
            List<Team> teams) {

        for (int i = 1; i <= 3; i++) {
            Team team = teams.get(i - 1);
            Robot robot = garantirRobo(
                    String.format("FX-%02d", i),
                    "Follow excepcional — cenário sem tentativa classificável.",
                    team);
            Registration registration =
                    garantirInscricao(competition, category, team, robot);

            for (int tomada = 1; tomada <= 3; tomada++) {
                for (int tentativa = 1; tentativa <= 3; tentativa++) {
                    garantirTentativaNaoClassificavel(
                            registration,
                            tomada,
                            tentativa,
                            Math.min(5, tentativa + tomada - 1));
                }
            }
        }
    }

    private void criarSumo(
            Competition competition,
            CompetitionCategory category,
            List<Team> teams) {

        for (int i = 1; i <= 7; i++) {
            Team team = teams.get((i - 1) % teams.size());
            Robot robot = garantirRobo(
                    String.format("S-%02d", i),
                    i <= 5
                            ? "Sumô apto para a chave de validação."
                            : "Sumô reservado para testes de inspeção.",
                    team);

            Registration registration =
                    garantirInscricao(competition, category, team, robot);

            if (i <= 5) {
                garantirInspecaoAprovada(
                        registration,
                        new BigDecimal("0." + (440 + i * 5)));
            }
        }
    }

    private Robot garantirRobo(String nome, String descricao, Team team) {
        Robot robot = robotRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(Robot::new);

        robot.setNome(nome);
        robot.setDescricao(descricao);
        robot.setTeam(team);
        robot.setAtivo(true);
        return robotRepository.save(robot);
    }

    private Registration garantirInscricao(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot) {

        Registration registration = registrationRepository.findAll().stream()
                .filter(item -> item.getCompetition().getId().equals(competition.getId()))
                .filter(item -> item.getCategory().getId().equals(category.getId()))
                .filter(item -> item.getRobot().getId().equals(robot.getId()))
                .findFirst()
                .orElseGet(Registration::new);

        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setObservacao("Inscrição de QA da ETAPA 4 / BLOCO 3.");
        registration.setAtivo(true);
        return registrationRepository.save(registration);
    }

    private void garantirTentativaNaoClassificavel(
            Registration registration,
            int tomada,
            int numeroTentativa,
            int checkpoints) {

        if (tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(
                registration.getId(), tomada, numeroTentativa)) {
            return;
        }

        TentativaSeguidorLinha tentativa = new TentativaSeguidorLinha();
        tentativa.setRegistration(registration);
        tentativa.setTomada(tomada);
        tentativa.setNumeroTentativa(numeroTentativa);
        tentativa.setTempoSegundos(null);
        tentativa.setCheckpointsAlcancados(checkpoints);
        tentativa.setPenalidadeSegundos(0);
        tentativa.setConcluida(false);
        tentativa.setValida(false);
        tentativa.setObservacao(
                "Pré-condição QA: tentativa não concluída para liberar resolução excepcional.");
        tentativaRepository.save(tentativa);
    }

    private void garantirInspecaoAprovada(
            Registration registration,
            BigDecimal peso) {

        if (inspecaoSumoService.estaAptaParaCompetir(registration.getId())) {
            return;
        }

        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(peso);
        dto.setAprovada(true);
        dto.setObservacao("Inspeção pré-aprovada pelo seed de QA do BLOCO 3.");
        inspecaoSumoService.registrar(dto);
    }

    private void garantirJuizes(Competition competition) {
        if (judgeRepository.findByCompetitionIdOrderByNomeAsc(competition.getId()).isEmpty()) {
            CompetitionJudge ativo = new CompetitionJudge();
            ativo.setCompetition(competition);
            ativo.setNome("Juiz B3 · Ativo");
            ativo.setAtivo(true);
            judgeRepository.save(ativo);

            CompetitionJudge inativo = new CompetitionJudge();
            inativo.setCompetition(competition);
            inativo.setNome("Juiz B3 · Inativo");
            inativo.setAtivo(false);
            judgeRepository.save(inativo);
        }
    }

    private void garantirDuasChavesDeSumo(
            Competition competition,
            CompetitionCategory category) {

        long existentes = bracketRepository
                .findByCompetitionIdOrderByDataCadastroDesc(competition.getId())
                .stream()
                .filter(item -> item.getCategory().getId().equals(category.getId()))
                .count();

        // Mantém o seed idempotente: no máximo uma histórica + uma vigente.
        if (existentes == 0) {
            bracketGenerationService.gerar(competition.getId(), category.getId());
            bracketGenerationService.gerar(competition.getId(), category.getId());
        } else if (existentes == 1) {
            bracketGenerationService.gerar(competition.getId(), category.getId());
        }
    }
}
