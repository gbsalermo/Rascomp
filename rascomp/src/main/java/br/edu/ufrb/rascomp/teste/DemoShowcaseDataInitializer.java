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
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.RoundSumoService;
import lombok.RequiredArgsConstructor;

/**
 * Cenário idempotente para demonstrações locais do RASCOMP.
 *
 * Ativado somente por rascomp.test-data.demo-showcase-enabled=true. O objetivo
 * não é substituir testes automatizados: ele prepara estados ricos para uma
 * apresentação, incluindo competição em andamento, histórico concluído,
 * ranking Follow, chave de 32 participantes, BYEs e portal participante.
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
    private final InspecaoSumoService inspecaoSumoService;
    private final BracketGenerationService bracketGenerationService;
    private final RoundSumoService roundSumoService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.storage.robot-images-dir:./uploads/robots}")
    private String robotImagesDirectory;

    @Override
    @Transactional
    public void run(String... args) {
        UserAccount organizacao = garantirUsuario(
                ORGANIZATION_EMAIL, "Organização Demo RAS", UserRole.ORGANIZACAO);
        UserAccount participante = garantirUsuario(
                PARTICIPANT_EMAIL, "Líder Demo", UserRole.PARTICIPANTE);

        Institution ras = garantirInstituicao("RAS-DEMO", "Instituição Demo RAS UFRB");
        Institution visitante = garantirInstituicao("ROBODEMO", "Instituto de Robótica Demo");

        Team teamParticipante = garantirEquipe("Equipe Demo RAS", ras, participante);
        Competitor lider = garantirCompetidor(
                "Líder Demo", PARTICIPANT_EMAIL, teamParticipante, participante);
        Competitor suporte = garantirCompetidor(
                "Suporte Demo", "suporte.demo@rascomp.local", teamParticipante, null);

        Robot chronos = garantirRobo("Chronos Demo", teamParticipante,
                "Seguidor de linha da equipe para demonstrar tomadas e ranking.");
        Robot titan = garantirRobo("Titan Demo", teamParticipante,
                "Mini Sumô usado para demonstrar partida vencida e progressão de chave.");
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
                live, followCategory, teamParticipante, chronos,
                Set.of(lider, suporte), StatusRegistration.APROVADA, participante, organizacao,
                "Demonstração: duas tomadas concluídas e terceira ainda disponível.");
        Registration titanSumo = garantirInscricao(
                live, miniCategory, teamParticipante, titan,
                Set.of(lider, suporte), StatusRegistration.APROVADA, participante, organizacao,
                "Demonstração: robô já venceu uma partida de Sumô.");

        prepararFollowAoVivo(live, followCategory, chronosFollow, ras, visitante, organizacao);
        prepararSumoAoVivo(live, miniCategory, titanSumo, ras, visitante, organizacao);
        prepararChaveComByes(live, byeCategory, visitante, organizacao);
        prepararPendenciasDashboard(live, followCategory, miniCategory, visitante, organizacao);
        prepararHistoricoCompleto(history, historyCategory, visitante, organizacao);

        System.out.println("============================================================");
        System.out.println("RASCOMP · CENARIO COMPLETO DE DEMONSTRACAO PRONTO");
        System.out.println("Competicao ao vivo: " + live.getNome() + " (#" + live.getId() + ")");
        System.out.println("Historico: " + history.getNome() + " (#" + history.getId() + ")");
        System.out.println("Participante: " + PARTICIPANT_EMAIL + " / " + DEMO_PASSWORD);
        System.out.println("Organizacao: " + ORGANIZATION_EMAIL + " / " + DEMO_PASSWORD);
        System.out.println("Destaques: Follow 2/3 tomadas, Sumô parcial, BYEs, chave 32 e histórico finalizado.");
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
        Institution institution = institutionRepository.findAll().stream()
                .filter(item -> sigla.equalsIgnoreCase(item.getSigla()))
                .findFirst().orElseGet(Institution::new);
        institution.setNome(nome);
        institution.setSigla(sigla);
        institution.setCidade("Cruz das Almas");
        institution.setEstado("BA");
        institution.setAtivo(true);
        return institutionRepository.save(institution);
    }

    private Team garantirEquipe(String nome, Institution institution, UserAccount responsavel) {
        Team team = teamRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst().orElseGet(Team::new);
        team.setNome(nome);
        team.setInstitution(institution);
        team.setResponsibleUser(responsavel);
        team.setAtivo(true);
        return teamRepository.save(team);
    }

    private Competitor garantirCompetidor(String nome, String email, Team team, UserAccount user) {
        Competitor competitor = competitorRepository.findAll().stream()
                .filter(item -> email.equalsIgnoreCase(item.getEmail()))
                .findFirst().orElseGet(Competitor::new);
        competitor.setNome(nome);
        competitor.setEmail(email);
        competitor.setTelefone("75999990000");
        competitor.setTeam(team);
        competitor.setUserAccount(user);
        competitor.setAtivo(true);
        return competitorRepository.save(competitor);
    }

    private Robot garantirRobo(String nome, Team team, String descricao) {
        Robot robot = robotRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .filter(item -> item.getTeam().getId().equals(team.getId()))
                .findFirst().orElseGet(Robot::new);
        robot.setNome(nome);
        robot.setDescricao(descricao);
        robot.setTeam(team);
        robot.setAtivo(true);
        return robotRepository.save(robot);
    }

    private CompetitionCategory garantirCategoria(String nome, Modalidade modalidade) {
        CompetitionCategory category = categoryRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(() -> CompetitionCategory.builder().build());
        category.setNome(nome);
        category.setDescricao("Categoria de demonstração do RASCOMP.");
        category.setModalidade(modalidade);
        category.setAtivo(true);
        return categoryRepository.save(category);
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
        Competition competition = buscarCompeticaoPorNome(LIVE_COMPETITION);
        competition.setNome(LIVE_COMPETITION);
        competition.setDescricao("Edição preparada para demonstração: operação em andamento, inscrições analisadas e provas parcialmente executadas.");
        competition.setInicioInscricoes(hoje.minusDays(30));
        competition.setFimInscricoes(hoje.minusDays(2));
        competition.setDataInicio(hoje.minusDays(1));
        competition.setDataFim(hoje.plusDays(1));
        competition.setStatus(StatusCompetition.EM_ANDAMENTO);
        competition.setAtivo(true);
        return competitionRepository.save(competition);
    }

    private Competition garantirCompeticaoHistorica() {
        Competition competition = buscarCompeticaoPorNome(HISTORY_COMPETITION);
        competition.setNome(HISTORY_COMPETITION);
        competition.setDescricao("Campeonato já encerrado para demonstrar chave completa de 32 robôs e consulta histórica.");
        competition.setInicioInscricoes(LocalDate.of(2025, 8, 1));
        competition.setFimInscricoes(LocalDate.of(2025, 9, 1));
        competition.setDataInicio(LocalDate.of(2025, 10, 10));
        competition.setDataFim(LocalDate.of(2025, 10, 12));
        competition.setStatus(StatusCompetition.FINALIZADA);
        competition.setAtivo(true);
        return competitionRepository.save(competition);
    }

    private Competition buscarCompeticaoPorNome(String nome) {
        return competitionRepository.findAll().stream()
                .filter(item -> nome.equalsIgnoreCase(item.getNome()))
                .findFirst().orElseGet(Competition::new);
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

        Registration registration = registrationRepository.findAll().stream()
                .filter(item -> item.getCompetition().getId().equals(competition.getId()))
                .filter(item -> item.getCategory().getId().equals(category.getId()))
                .filter(item -> item.getRobot().getId().equals(robot.getId()))
                .findFirst().orElseGet(Registration::new);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(new LinkedHashSet<>(competitors));
        registration.setStatus(status);
        registration.setObservacao(observacao);
        registration.setRequestedByUser(requestedBy);
        registration.setAtivo(status != StatusRegistration.CANCELADA);
        if (status == StatusRegistration.APROVADA || status == StatusRegistration.REJEITADA) {
            registration.setReviewedByUser(reviewedBy);
            if (registration.getReviewedAt() == null) registration.setReviewedAt(LocalDateTime.now().minusHours(6));
        }
        return registrationRepository.save(registration);
    }

    private void prepararFollowAoVivo(
            Competition competition,
            CompetitionCategory category,
            Registration chronos,
            Institution ras,
            Institution visitante,
            UserAccount organizacao) {

        // Chronos já completou duas das três tomadas.
        garantirTentativa(chronos, 1, 1, "47.820", 5, 0, true, true, "Tomada 1 · tentativa de aquecimento.");
        garantirTentativa(chronos, 1, 2, "44.610", 5, 1, true, true, "Tomada 1 · uma penalidade.");
        garantirTentativa(chronos, 1, 3, "43.950", 5, 0, true, true, "Melhor tentativa da tomada 1.");
        garantirTentativa(chronos, 2, 1, "42.780", 5, 0, true, true, "Tomada 2 · evolução de tempo.");
        garantirTentativa(chronos, 2, 2, "41.930", 5, 0, true, true, "Melhor marca atual do Chronos.");
        garantirTentativa(chronos, 2, 3, "45.300", 4, 0, false, true, "Tentativa interrompida.");

        String[] nomes = { "Velocity Demo", "LineHunter Demo", "Pulsar Demo", "Vector Demo", "Orion Track Demo" };
        String[] melhores = { "38.420", "40.210", "43.150", "45.800", "49.100" };
        for (int i = 0; i < nomes.length; i++) {
            Team team = garantirEquipe("Equipe Follow Demo " + (i + 1), i % 2 == 0 ? ras : visitante, null);
            Robot robot = garantirRobo(nomes[i], team, "Robô de cenário para ranking Follow.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Inscrição aprovada para ranking pré-estabelecido.");
            garantirTentativa(reg, 1, 1, melhores[i], 5, 0, true, true, "Marca pré-estabelecida de demonstração.");
            garantirTentativa(reg, 1, 2, String.valueOf(new BigDecimal(melhores[i]).add(new BigDecimal("2.750"))), 5, 0, true, true, null);
            if (i < 3) {
                garantirTentativa(reg, 2, 1, String.valueOf(new BigDecimal(melhores[i]).add(new BigDecimal("1.100"))), 5, i == 1 ? 1 : 0, true, true, null);
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
            Robot robot = garantirRobo("MiniBot Demo " + i, team, "Robô Mini Sumô de demonstração.");
            regs.add(garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Inscrição apta para chave ao vivo."));
        }
        regs.forEach(reg -> garantirInspecaoAprovada(reg, "0.480"));

        Bracket bracket = chaveAtual(competition, category);
        if (bracket == null) {
            BracketDTO dto = bracketGenerationService.gerar(competition.getId(), category.getId());
            bracket = bracketRepository.findById(dto.getId()).orElseThrow();
        }

        // Garante que o Titan tenha uma vitória registrada.
        Match titanMatch = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId()).stream()
                .filter(match -> match.getRodada() == 1)
                .filter(match -> match.getStatus() == StatusMatch.AGENDADA || match.getStatus() == StatusMatch.EM_ANDAMENTO)
                .filter(match -> titan.getId().equals(id(match.getRegistrationA())) || titan.getId().equals(id(match.getRegistrationB())))
                .findFirst().orElse(null);
        if (titanMatch != null && !partidaPossuiRounds(titanMatch)) {
            registrarVitoria(titanMatch, titan, false, 0, 1, "Titan abriu a demonstração com vitória normal.");
            registrarVitoria(titanMatch, titan, true, 0, 0, "Segundo round por Suicídio/WO do adversário.");
        }

        // Avança parte dos outros confrontos para deixar o campeonato visualmente no meio.
        int concluidasExtras = 0;
        for (Match match : matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId())) {
            if (match.getRodada() != 1 || match.getStatus() != StatusMatch.AGENDADA || partidaPossuiRounds(match)) continue;
            if (concluidasExtras >= 2) break;
            registrarVitoria(match, match.getRegistrationA(), false, 1, 0, "Round com penalidade demonstrativa.");
            registrarVitoria(match, match.getRegistrationA(), false, 0, 0, null);
            concluidasExtras++;
        }
    }

    private void prepararChaveComByes(
            Competition competition,
            CompetitionCategory category,
            Institution institution,
            UserAccount organizacao) {

        List<Registration> regs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Team team = garantirEquipe("Equipe 3kg Demo " + i, institution, null);
            Robot robot = garantirRobo("Heavy Demo " + i, team, "Robô 3 kg para demonstrar BYE.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Chave propositalmente não potência de dois.");
            garantirInspecaoAprovada(reg, "2.850");
            regs.add(reg);
        }
        if (chaveAtual(competition, category) == null) {
            bracketGenerationService.gerar(competition.getId(), category.getId());
        }
    }

    private void prepararPendenciasDashboard(
            Competition competition,
            CompetitionCategory followCategory,
            CompetitionCategory sumoCategory,
            Institution institution,
            UserAccount organizacao) {

        for (int i = 1; i <= 3; i++) {
            Team team = garantirEquipe("Equipe Pendente Demo " + i, institution, null);
            Robot robot = garantirRobo("PendingBot Demo " + i, team, "Aguardando análise da organização.");
            garantirInscricao(
                    competition,
                    i % 2 == 0 ? followCategory : sumoCategory,
                    team,
                    robot,
                    Set.of(),
                    StatusRegistration.PENDENTE,
                    null,
                    organizacao,
                    "Inscrição deixada pendente para demonstrar aprovação em tempo real.");
        }

        Team teamRejeitada = garantirEquipe("Equipe Rejeitada Demo", institution, null);
        Robot rejeitado = garantirRobo("RejectedBot Demo", teamRejeitada, "Inscrição já revisada e rejeitada.");
        garantirInscricao(
                competition, sumoCategory, teamRejeitada, rejeitado, Set.of(), StatusRegistration.REJEITADA,
                null, organizacao, "Exemplo de inscrição rejeitada.");
    }

    private void prepararHistoricoCompleto(
            Competition competition,
            CompetitionCategory category,
            Institution institution,
            UserAccount organizacao) {

        for (int i = 1; i <= 32; i++) {
            Team team = garantirEquipe(String.format("Histórico Team %02d", i), institution, null);
            Robot robot = garantirRobo(String.format("LegacyBot %02d", i), team, "Participante da chave histórica de 32 robôs.");
            Registration reg = garantirInscricao(
                    competition, category, team, robot, Set.of(), StatusRegistration.APROVADA,
                    null, organizacao, "Inscrição histórica aprovada.");
            garantirInspecaoAprovada(reg, "0.470");
        }

        Bracket bracket = chaveAtual(competition, category);
        if (bracket == null) {
            BracketDTO dto = bracketGenerationService.gerar(competition.getId(), category.getId());
            bracket = bracketRepository.findById(dto.getId()).orElseThrow();
        }

        // Completa 16 avos, oitavas, quartas, semifinal e final usando o motor oficial.
        for (int rodada = 1; rodada <= 5; rodada++) {
            List<Match> partidas = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId()).stream()
                    .filter(match -> match.getRodada() == rodada)
                    .toList();
            for (Match match : partidas) {
                Match atual = matchRepository.findById(match.getId()).orElseThrow();
                if (atual.getStatus() == StatusMatch.FINALIZADA || atual.getStatus() == StatusMatch.BYE) continue;
                if (atual.getRegistrationA() == null || atual.getRegistrationB() == null) continue;
                Registration winner = atual.getRegistrationA();
                if (!partidaPossuiRounds(atual)) {
                    boolean wo = rodada == 1 && atual.getOrdem() == 3;
                    registrarVitoria(atual, winner, wo, atual.getOrdem() % 4 == 0 ? 1 : 0, 0,
                            wo ? "Histórico: vitória por Suicídio/WO." : "Histórico de campeonato finalizado.");
                    registrarVitoria(atual, winner, false, 0, 0, null);
                }
            }
        }
    }

    private void garantirInspecaoAprovada(Registration registration, String peso) {
        try {
            if (inspecaoSumoService.estaAptaParaCompetir(registration.getId())) return;
        } catch (Exception ignored) {
            // A criação abaixo produzirá o estado esperado ou evidenciará a inconsistência.
        }
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal(peso));
        dto.setObservacao("Inspeção aprovada automaticamente pelo profile de demonstração.");
        inspecaoSumoService.registrar(dto);
    }

    private Bracket chaveAtual(Competition competition, CompetitionCategory category) {
        return bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(competition.getId(), category.getId())
                .stream().filter(item -> Boolean.TRUE.equals(item.getAtivo())).findFirst().orElse(null);
    }

    private boolean partidaPossuiRounds(Match match) {
        return match.getStatus() == StatusMatch.FINALIZADA || match.getStatus() == StatusMatch.EM_ANDAMENTO;
    }

    private void registrarVitoria(
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

    private Long id(Registration registration) {
        return registration == null ? null : registration.getId();
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
                registration.getId(), tomada, numeroTentativa)) return;
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
            throw new IllegalStateException("Não foi possível gerar a imagem de demonstração do robô.", ex);
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
