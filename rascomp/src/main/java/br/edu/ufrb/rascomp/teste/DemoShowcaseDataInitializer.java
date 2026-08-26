package br.edu.ufrb.rascomp.teste;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotImage;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotImageRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.RoundSumoService;
import lombok.RequiredArgsConstructor;

/**
 * Cenário idempotente e opt-in para apresentação local do RASCOMP.
 *
 * Inclui:
 * - competição em andamento com datas relativas ao dia atual;
 * - inscrições aprovadas, pendentes e rejeitada;
 * - Follow Line com ranking pré-estabelecido e tomadas históricas;
 * - Sumô parcialmente executado, penalidade e Suicídio/WO;
 * - chave não potência de dois para demonstrar BYEs;
 * - competição encerrada com chave completa de 32 robôs (16 avos até final);
 * - participante líder com dois robôs, fotos, histórico Follow e vitória no Sumô.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rascomp.test-data.demo-showcase-enabled", havingValue = "true")
public class DemoShowcaseDataInitializer implements CommandLineRunner {

    public static final String PARTICIPANT_EMAIL = "lider.demo@rascomp.local";
    public static final String ORGANIZATION_EMAIL = "organizacao.demo@rascomp.local";
    public static final String DEMO_PASSWORD = "Rascomp@2026";

    private static final String LIVE_COMPETITION = "RRC 2026 · Demonstração ao vivo";
    private static final String HISTORY_COMPETITION = "RRC 2025 · Histórico completo";
    private static final String FOLLOW_CATEGORY = "DEMO · Seguidor de Linha";
    private static final String MINI_CATEGORY = "DEMO · Mini Sumô RC";
    private static final String BYE_CATEGORY = "DEMO · Sumô 3 kg RC · BYEs";
    private static final String HISTORY_SUMO_CATEGORY = "DEMO · Mini Sumô · 32 robôs";

    private final UserAccountRepository userAccountRepository;
    private final InstitutionRepository institutionRepository;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotImageRepository robotImageRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final BracketRepository bracketRepository;
    private final MatchRepository matchRepository;
    private final RoundSumoRepository roundRepository;
    private final InspecaoSumoService inspecaoSumoService;
    private final BracketGenerationService bracketGenerationService;
    private final RoundSumoService roundSumoService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.storage.robot-images-dir:./uploads/robots}")
    private String robotImagesDirectory;

    @Override
    @Transactional
    public void run(String... args) {
        UserAccount organizacao = garantirUsuario(ORGANIZATION_EMAIL, "Organização Demo RAS", UserRole.ORGANIZACAO);
        UserAccount participante = garantirUsuario(PARTICIPANT_EMAIL, "Líder Demo", UserRole.PARTICIPANTE);

        Institution ras = garantirInstituicao("RAS-DEMO", "Instituição Demo RAS UFRB");
        Institution visitante = garantirInstituicao("ROBODEMO", "Instituto de Robótica Demo");

        Team teamParticipante = garantirEquipe("Equipe Demo RAS", ras, participante);
        Competitor lider = garantirCompetidor("Líder Demo", PARTICIPANT_EMAIL, teamParticipante, participante);
        Competitor suporte = garantirCompetidor("Suporte Demo", "suporte.demo@rascomp.local", teamParticipante, null);

        Robot chronos = garantirRobo("Chronos Demo", teamParticipante,
                "Seguidor de linha para demonstrar tomadas, tentativas e ranking.");
        Robot titan = garantirRobo("Titan Demo", teamParticipante,
                "Mini Sumô para demonstrar partida vencida e progressão.");
        garantirFotoDemo(chronos, "CHRONOS", new Color(79, 25, 103));
        garantirFotoDemo(titan, "TITAN", new Color(159, 15, 59));

        CompetitionCategory followCategory = garantirCategoria(FOLLOW_CATEGORY, Modalidade.FOLLOW_LINE);
        CompetitionCategory miniCategory = garantirCategoria(MINI_CATEGORY, Modalidade.SUMO);
        CompetitionCategory byeCategory = garantirCategoria(BYE_CATEGORY, Modalidade.SUMO);
        CompetitionCategory historyCategory = garantirCategoria(HISTORY_SUMO_CATEGORY, Modalidade.SUMO);
        garantirConfigFollow(followCategory);
        garantirConfigSumo(miniCategory, "0.500");
        garantirConfigSumo(byeCategory, "3.000");
        garantirConfigSumo(historyCategory, "0.500");

        Competition live = garantirCompeticaoAoVivo();
        Competition history = garantirCompeticaoHistorica();

        Registration chronosFollow = garantirInscricao(
                live, followCategory, teamParticipante, chronos, Set.of(lider, suporte),
                StatusRegistration.APROVADA, participante, organizacao,
                "Duas tomadas concluídas; terceira ainda disponível.");
        Registration titanSumo = garantirInscricao(
                live, miniCategory, teamParticipante, titan, Set.of(lider, suporte),
                StatusRegistration.APROVADA, participante, organizacao,
                "Robô já venceu uma partida do chaveamento ao vivo.");

        prepararFollowAoVivo(live, followCategory, chronosFollow, ras, visitante, organizacao);
        prepararSumoAoVivo(live, miniCategory, titanSumo, ras, visitante, organizacao);
        prepararChaveComByes(live, byeCategory, visitante, organizacao);
        prepararPendenciasDashboard(live, followCategory, miniCategory, visitante, organizacao);
        prepararHistoricoCompleto(history, historyCategory, visitante, organizacao);

        System.out.println("============================================================");
        System.out.println("RASCOMP · CENARIO COMPLETO DE DEMONSTRACAO PRONTO");
        System.out.println("Ao vivo: " + live.getNome() + " (#" + live.getId() + ")");
        System.out.println("Histórico: " + history.getNome() + " (#" + history.getId() + ")");
        System.out.println("PARTICIPANTE: " + PARTICIPANT_EMAIL + " / " + DEMO_PASSWORD);
        System.out.println("ORGANIZACAO: " + ORGANIZATION_EMAIL + " / " + DEMO_PASSWORD);
        System.out.println("Destaques: Follow 2/3 tomadas, Sumô parcial, BYEs e chave completa de 32 robôs.");
        System.out.println("============================================================");
    }

    private UserAccount garantirUsuario(String email, String nome, UserRole role) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email).orElseGet(UserAccount::new);
        user.setNome(nome);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setAtivo(true);
        return userAccountRepository.save(user);
    }

    private Institution garantirInstituicao(String sigla, String nome) {
        Institution item = institutionRepository.findAll().stream()
                .filter(i -> sigla.equalsIgnoreCase(i.getSigla())).findFirst().orElseGet(Institution::new);
        item.setNome(nome);
        item.setSigla(sigla);
        item.setCidade("Cruz das Almas");
        item.setEstado("BA");
        item.setAtivo(true);
        return institutionRepository.save(item);
    }

    private Team garantirEquipe(String nome, Institution institution, UserAccount responsavel) {
        Team item = teamRepository.findAll().stream()
                .filter(i -> nome.equalsIgnoreCase(i.getNome())).findFirst().orElseGet(Team::new);
        item.setNome(nome);
        item.setInstitution(institution);
        item.setResponsibleUser(responsavel);
        item.setAtivo(true);
        return teamRepository.save(item);
    }

    private Competitor garantirCompetidor(String nome, String email, Team team, UserAccount user) {
        Competitor item = competitorRepository.findAll().stream()
                .filter(i -> email.equalsIgnoreCase(i.getEmail())).findFirst().orElseGet(Competitor::new);
        item.setNome(nome);
        item.setEmail(email);
        item.setTelefone("75999990000");
        item.setTeam(team);
        item.setUserAccount(user);
        item.setAtivo(true);
        return competitorRepository.save(item);
    }

    private Robot garantirRobo(String nome, Team team, String descricao) {
        Robot item = robotRepository.findAll().stream()
                .filter(i -> nome.equalsIgnoreCase(i.getNome()))
                .filter(i -> i.getTeam().getId().equals(team.getId()))
                .findFirst().orElseGet(Robot::new);
        item.setNome(nome);
        item.setDescricao(descricao);
        item.setTeam(team);
        item.setAtivo(true);
        return robotRepository.save(item);
    }

    private CompetitionCategory garantirCategoria(String nome, Modalidade modalidade) {
        CompetitionCategory item = categoryRepository.findAll().stream()
                .filter(i -> nome.equalsIgnoreCase(i.getNome())).findFirst()
                .orElseGet(() -> CompetitionCategory.builder().build());
        item.setNome(nome);
        item.setDescricao("Categoria preparada pelo profile local de demonstração.");
        item.setModalidade(modalidade);
        item.setAtivo(true);
        return categoryRepository.save(item);
    }

    private void garantirConfigFollow(CompetitionCategory category) {
        ConfigFollow config = configFollowRepository.findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigFollow.builder().competitionCategory(category).build());
        config.setNumeroTomadas(3);
        config.setTentativasPorTomada(3);
        config.setMaxTempoSegundos(120);
        config.setNumeroCheckpoints(5);
        configFollowRepository.save(config);
    }

    private void garantirConfigSumo(CompetitionCategory category, String pesoMax) {
        ConfigSumo config = configSumoRepository.findByCompetitionCategoryId(category.getId())
                .orElseGet(() -> ConfigSumo.builder().competitionCategory(category).build());
        config.setPesoMax(new BigDecimal(pesoMax));
        config.setExigeInspecao(true);
        config.setMaxTentativasInspecao(2);
        config.setNumeroRounds(3);
        config.setRoundsParaVencer(2);
        config.setPermiteRoundDesempate(true);
        configSumoRepository.save(config);
    }

    private Competition garantirCompeticaoAoVivo() {
        LocalDate hoje = LocalDate.now();
        Competition item = competicaoPorNome(LIVE_COMPETITION);
        item.setNome(LIVE_COMPETITION);
        item.setDescricao("Edição em andamento para mostrar dashboard, aprovação, Follow Line e Sumô ao vivo.");
        item.setInicioInscricoes(hoje.minusDays(30));
        item.setFimInscricoes(hoje.minusDays(2));
        item.setDataInicio(hoje.minusDays(1));
        item.setDataFim(hoje.plusDays(1));
        item.setStatus(StatusCompetition.EM_ANDAMENTO);
        item.setAtivo(true);
        return competitionRepository.save(item);
    }

    private Competition garantirCompeticaoHistorica() {
        Competition item = competicaoPorNome(HISTORY_COMPETITION);
        item.setNome(HISTORY_COMPETITION);
        item.setDescricao("Campeonato encerrado para demonstrar histórico e chave completa de 32 robôs.");
        item.setInicioInscricoes(LocalDate.of(2025, 8, 1));
        item.setFimInscricoes(LocalDate.of(2025, 9, 1));
        item.setDataInicio(LocalDate.of(2025, 10, 10));
        item.setDataFim(LocalDate.of(2025, 10, 12));
        item.setStatus(StatusCompetition.FINALIZADA);
        item.setAtivo(true);
        return competitionRepository.save(item);
    }

    private Competition competicaoPorNome(String nome) {
        return competitionRepository.findAll().stream()
                .filter(i -> nome.equalsIgnoreCase(i.getNome())).findFirst().orElseGet(Competition::new);
    }

    private Registration garantirInscricao(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot,
            Set<Competitor> competitors,
            StatusRegistration status,
            UserAccount requestedBy,
            UserAccount reviewedBy,
            String observacao) {
        Registration item = registrationRepository.findAll().stream()
                .filter(i -> i.getCompetition().getId().equals(competition.getId()))
                .filter(i -> i.getCategory().getId().equals(category.getId()))
                .filter(i -> i.getRobot().getId().equals(robot.getId()))
                .findFirst().orElseGet(Registration::new);
        item.setCompetition(competition);
        item.setCategory(category);
        item.setTeam(team);
        item.setRobot(robot);
        item.setCompetitors(new LinkedHashSet<>(competitors));
        item.setStatus(status);
        item.setObservacao(observacao);
        item.setRequestedByUser(requestedBy);
        item.setAtivo(status != StatusRegistration.CANCELADA);
        if (status == StatusRegistration.APROVADA || status == StatusRegistration.REJEITADA) {
            item.setReviewedByUser(reviewedBy);
            if (item.getReviewedAt() == null) item.setReviewedAt(LocalDateTime.now().minusHours(6));
        }
        return registrationRepository.save(item);
    }

    private void prepararFollowAoVivo(
            Competition competition,
            CompetitionCategory category,
            Registration chronos,
            Institution ras,
            Institution visitante,
            UserAccount organizacao) {
        // Chronos: duas tomadas completas e a terceira livre para demonstrar ao vivo.
        tentativa(chronos, 1, 1, "47.820", 5, 0, true, true, "Aquecimento.");
        tentativa(chronos, 1, 2, "44.610", 5, 1, true, true, "Uma penalidade.");
        tentativa(chronos, 1, 3, "43.950", 5, 0, true, true, "Melhor da tomada 1.");
        tentativa(chronos, 2, 1, "42.780", 5, 0, true, true, "Evolução de tempo.");
        tentativa(chronos, 2, 2, "41.930", 5, 0, true, true, "Melhor marca atual.");
        tentativa(chronos, 2, 3, "45.300", 4, 0, false, true, "Tentativa interrompida.");

        String[] nomes = { "Velocity Demo", "LineHunter Demo", "Pulsar Demo", "Vector Demo", "Orion Track Demo" };
        String[] marcas = { "38.420", "40.210", "43.150", "45.800", "49.100" };
        for (int i = 0; i < nomes.length; i++) {
            Team team = garantirEquipe("Equipe Follow Demo " + (i + 1), i % 2 == 0 ? ras : visitante, null);
            Robot robot = garantirRobo(nomes[i], team, "Robô de ranking pré-estabelecido.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Ranking Follow de demonstração.");
            tentativa(reg, 1, 1, marcas[i], 5, 0, true, true, "Marca principal.");
            tentativa(reg, 1, 2, new BigDecimal(marcas[i]).add(new BigDecimal("2.750")).toPlainString(), 5, 0, true, true, null);
            if (i < 3) {
                tentativa(reg, 2, 1, new BigDecimal(marcas[i]).add(new BigDecimal("1.100")).toPlainString(), 5, i == 1 ? 1 : 0, true, true, null);
            }
        }
    }

    private void prepararSumoAoVivo(
            Competition competition,
            CompetitionCategory category,
            Registration titan,
            Institution ras,
            Institution visitante,
            UserAccount organizacao) {
        List<Registration> regs = new ArrayList<>();
        regs.add(titan);
        for (int i = 1; i <= 7; i++) {
            Team team = garantirEquipe("Equipe Mini Demo " + i, i % 2 == 0 ? ras : visitante, null);
            Robot robot = garantirRobo("MiniBot Demo " + i, team, "Mini Sumô de demonstração.");
            regs.add(garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Chave ao vivo."));
        }
        regs.forEach(reg -> inspecaoAprovada(reg, "0.480"));

        Bracket bracket = garantirChave(competition, category);
        Long bracketId = bracket.getId();
        Match titanMatch = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId).stream()
                .filter(m -> m.getRodada() == 1)
                .filter(m -> titan.getId().equals(id(m.getRegistrationA())) || titan.getId().equals(id(m.getRegistrationB())))
                .findFirst().orElse(null);
        if (titanMatch != null && roundRepository.countByMatchId(titanMatch.getId()) == 0) {
            vitoria(titanMatch, titan, false, 0, 1, "Titan venceu o primeiro round.");
            vitoria(titanMatch, titan, true, 0, 0, "Suicídio/WO do adversário.");
        }

        int completas = 0;
        for (Match match : matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId)) {
            Match atual = matchRepository.findById(match.getId()).orElseThrow();
            if (atual.getRodada() != 1 || atual.getStatus() != StatusMatch.AGENDADA) continue;
            if (roundRepository.countByMatchId(atual.getId()) > 0 || completas >= 2) continue;
            vitoria(atual, atual.getRegistrationA(), false, 1, 0, "Round com penalidade demonstrativa.");
            vitoria(atual, atual.getRegistrationA(), false, 0, 0, null);
            completas++;
        }
    }

    private void prepararChaveComByes(
            Competition competition,
            CompetitionCategory category,
            Institution institution,
            UserAccount organizacao) {
        for (int i = 1; i <= 10; i++) {
            Team team = garantirEquipe("Equipe 3kg Demo " + i, institution, null);
            Robot robot = garantirRobo("Heavy Demo " + i, team, "Robô 3 kg para demonstrar BYE.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Dez participantes geram chave de 16 com seis BYEs.");
            inspecaoAprovada(reg, "2.850");
        }
        garantirChave(competition, category);
    }

    private void prepararPendenciasDashboard(
            Competition competition,
            CompetitionCategory follow,
            CompetitionCategory sumo,
            Institution institution,
            UserAccount organizacao) {
        for (int i = 1; i <= 3; i++) {
            Team team = garantirEquipe("Equipe Pendente Demo " + i, institution, null);
            Robot robot = garantirRobo("PendingBot Demo " + i, team, "Aguardando análise.");
            garantirInscricao(
                    competition, i % 2 == 0 ? follow : sumo, team, robot, Set.of(),
                    StatusRegistration.PENDENTE, null, organizacao,
                    "Deixada pendente para demonstrar aprovação em tempo real.");
        }
        Team team = garantirEquipe("Equipe Rejeitada Demo", institution, null);
        Robot robot = garantirRobo("RejectedBot Demo", team, "Inscrição rejeitada.");
        garantirInscricao(
                competition, sumo, team, robot, Set.of(), StatusRegistration.REJEITADA,
                null, organizacao, "Exemplo de revisão já concluída.");
    }

    private void prepararHistoricoCompleto(
            Competition competition,
            CompetitionCategory category,
            Institution institution,
            UserAccount organizacao) {
        for (int i = 1; i <= 32; i++) {
            Team team = garantirEquipe(String.format("Histórico Team %02d", i), institution, null);
            Robot robot = garantirRobo(String.format("LegacyBot %02d", i), team, "Participante da chave de 32.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Inscrição histórica.");
            inspecaoAprovada(reg, "0.470");
        }

        Bracket bracket = garantirChave(competition, category);
        Long bracketId = bracket.getId();
        for (int rodada = 1; rodada <= 5; rodada++) {
            List<Match> partidas = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId).stream()
                    .filter(m -> m.getRodada() == rodada).toList();
            for (Match item : partidas) {
                Match match = matchRepository.findById(item.getId()).orElseThrow();
                if (match.getStatus() == StatusMatch.FINALIZADA || match.getStatus() == StatusMatch.BYE) continue;
                if (match.getRegistrationA() == null || match.getRegistrationB() == null) continue;
                if (roundRepository.countByMatchId(match.getId()) > 0) continue;
                Registration winner = match.getRegistrationA();
                boolean wo = rodada == 1 && match.getOrdem() == 3;
                vitoria(match, winner, wo, match.getOrdem() % 4 == 0 ? 1 : 0, 0,
                        wo ? "Histórico: Suicídio/WO." : "Histórico de campeonato finalizado.");
                vitoria(match, winner, false, 0, 0, null);
            }
        }
    }

    private Bracket garantirChave(Competition competition, CompetitionCategory category) {
        Bracket atual = bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(competition.getId(), category.getId())
                .stream().filter(b -> Boolean.TRUE.equals(b.getAtivo())).findFirst().orElse(null);
        if (atual != null) return atual;
        BracketDTO dto = bracketGenerationService.gerar(competition.getId(), category.getId());
        return bracketRepository.findById(dto.getId()).orElseThrow();
    }

    private void inspecaoAprovada(Registration registration, String peso) {
        try {
            if (inspecaoSumoService.estaAptaParaCompetir(registration.getId())) return;
        } catch (Exception ignored) {
            // segue para criar inspeção válida
        }
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal(peso));
        dto.setObservacao("Inspeção aprovada pelo profile de demonstração.");
        inspecaoSumoService.registrar(dto);
    }

    private void vitoria(
            Match match,
            Registration winner,
            boolean wo,
            int penalidadesA,
            int penalidadesB,
            String observacao) {
        RoundSumoDTO round = new RoundSumoDTO();
        round.setMatchId(match.getId());
        round.setWinnerRegistrationId(winner.getId());
        round.setStatus(StatusRoundSumo.FINALIZADO);
        round.setMotivoResultado(wo ? MotivoResultadoRoundSumo.SUICIDIO_WO : MotivoResultadoRoundSumo.DISPUTA);
        round.setPenalidadesA(penalidadesA);
        round.setPenalidadesB(penalidadesB);
        round.setObservacao(observacao);
        roundSumoService.registrar(round);
    }

    private void tentativa(
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
                registration.getId(), tomada, numeroTentativa)) return;
        TentativaSeguidorLinha item = new TentativaSeguidorLinha();
        item.setRegistration(registration);
        item.setTomada(tomada);
        item.setNumeroTentativa(numeroTentativa);
        item.setTempoSegundos(new BigDecimal(tempo));
        item.setCheckpointsAlcancados(checkpoints);
        item.setPenalidadeSegundos(penalidade);
        item.setConcluida(concluida);
        item.setValida(valida);
        item.setObservacao(observacao);
        tentativaRepository.save(item);
    }

    private Long id(Registration registration) {
        return registration == null ? null : registration.getId();
    }

    private void garantirFotoDemo(Robot robot, String label, Color background) {
        if (robotImageRepository.findFirstByRobotIdAndPrincipalTrueAndAtivoTrue(robot.getId()).isPresent()) return;
        String storageKey = robot.getId() + "/demo-showcase.png";
        Path root = Paths.get(robotImagesDirectory).toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Caminho de imagem demo inválido.");
        try {
            Files.createDirectories(target.getParent());
            BufferedImage image = new BufferedImage(640, 420, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background);
            g.fillRect(0, 0, 640, 420);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillOval(390, -120, 360, 360);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            g.drawString(label, 52, 210);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            g.drawString("RASCOMP · DEMO ROBOT", 54, 250);
            g.dispose();
            ImageIO.write(image, "png", target.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar a imagem de demonstração.", ex);
        }
        RobotImage image = new RobotImage();
        image.setRobot(robot);
        image.setStorageKey(storageKey);
        image.setOriginalFilename(label.toLowerCase() + "-demo.png");
        image.setContentType("image/png");
        image.setPrincipal(true);
        image.setOrdem(0);
        image.setAtivo(true);
        robotImageRepository.save(image);
    }
}
