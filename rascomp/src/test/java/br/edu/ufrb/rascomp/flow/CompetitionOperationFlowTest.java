package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.FollowTakeScheduleDTO;
import br.edu.ufrb.rascomp.dto.FollowTakeScheduleEntryDTO;
import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.dto.MatchAgendaDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.RegistrationStatusChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusChamadaFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.CompetitionAgendaService;
import br.edu.ufrb.rascomp.service.CompetitionResultsService;
import br.edu.ufrb.rascomp.service.FollowTakeScheduleService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.MatchResultService;
import br.edu.ufrb.rascomp.service.MatchService;
import br.edu.ufrb.rascomp.service.RegistrationService;
import br.edu.ufrb.rascomp.service.RegistrationStatusHistoryService;
import br.edu.ufrb.rascomp.service.TentativaSeguidorLinhaService;

@SpringBootTest
@ActiveProfiles("flowtest")
class CompetitionOperationFlowTest extends IntegrationFlowTestSupport {

    @Autowired private FollowTakeScheduleService followScheduleService;
    @Autowired private TentativaSeguidorLinhaService tentativaService;
    @Autowired private CompetitionAgendaService agendaService;
    @Autowired private CompetitionResultsService resultsService;
    @Autowired private InspecaoSumoService inspecaoService;
    @Autowired private BracketGenerationService bracketGenerationService;
    @Autowired private RegistrationService registrationService;
    @Autowired private MatchResultService matchResultService;
    @Autowired private MatchService matchService;
    @Autowired private RegistrationStatusHistoryService statusHistoryService;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void agendaFollowDeveCriarFilaESincronizarConclusaoDaTomada() {
        organizacaoAutenticada();

        Competition competition = competition(StatusCompetition.EM_ANDAMENTO);
        CompetitionCategory category = followCategory();
        Team team = team();
        Registration registration = approvedRegistration(
                competition, category, team, robot(team));

        FollowTakeScheduleDTO schedule = new FollowTakeScheduleDTO();
        schedule.setCompetitionId(competition.getId());
        schedule.setCategoryId(category.getId());
        schedule.setTomada(1);
        schedule.setDataHora(LocalDateTime.now().plusHours(1));
        schedule.setPista("Pista A");
        schedule.setOrdemExecucao(1);
        schedule.setStatus(StatusChamadaFollow.AGENDADA);

        FollowTakeScheduleDTO criada = followScheduleService.criar(schedule);
        var filaInicial = followScheduleService.listarFila(criada.getId());

        assertEquals(1, filaInicial.size());
        assertEquals(StatusConvocacaoFollow.AGUARDANDO, filaInicial.get(0).getStatus());
        assertEquals(registration.getId(), filaInicial.get(0).getRegistrationId());

        FollowTakeScheduleEntryDTO conclusaoManual = new FollowTakeScheduleEntryDTO();
        conclusaoManual.setOrdemConvocacao(1);
        conclusaoManual.setStatus(StatusConvocacaoFollow.CONCLUIDA);
        assertThrows(
                IllegalArgumentException.class,
                () -> followScheduleService.atualizarConvocacao(
                        filaInicial.get(0).getId(),
                        conclusaoManual));

        FollowTakeScheduleEntryDTO convocar = new FollowTakeScheduleEntryDTO();
        convocar.setOrdemConvocacao(1);
        convocar.setStatus(StatusConvocacaoFollow.CONVOCADA);
        var convocada = followScheduleService.atualizarConvocacao(
                filaInicial.get(0).getId(),
                convocar);
        assertEquals(StatusConvocacaoFollow.CONVOCADA, convocada.getStatus());

        for (int numero = 1; numero <= 3; numero++) {
            tentativaService.criar(tentativa(registration, 1, numero, "40.000"));
        }

        var filaFinal = followScheduleService.listarFila(criada.getId());
        assertEquals(StatusConvocacaoFollow.CONCLUIDA, filaFinal.get(0).getStatus());
        assertEquals(StatusChamadaFollow.FINALIZADA,
                followScheduleService.buscarPorId(criada.getId()).getStatus());
        assertThrows(
                IllegalArgumentException.class,
                () -> followScheduleService.sincronizarFila(criada.getId()));

        var agenda = agendaService.listar(competition.getId());
        assertTrue(agenda.stream().anyMatch(item ->
                "FOLLOW_TAKE".equals(item.getTipo())
                        && item.getSourceId().equals(criada.getId())
                        && Integer.valueOf(1).equals(item.getConcluidos())));

        var parcial = resultsService.listar(competition.getId());
        assertEquals(1, parcial.size());
        assertEquals("PENDENTE", parcial.get(0).getStatus());

        for (int tomada = 2; tomada <= 3; tomada++) {
            for (int numero = 1; numero <= 3; numero++) {
                tentativaService.criar(tentativa(
                        registration,
                        tomada,
                        numero,
                        tomada == 2 ? "39.000" : "38.000"));
            }
        }

        var resultado = resultsService.listar(competition.getId());
        assertEquals(1, resultado.size());
        assertEquals("CONCLUIDO", resultado.get(0).getStatus());
        assertEquals(registration.getId(), resultado.get(0).getWinnerRegistrationId());
    }

    @Test
    void agendaSumoNaoDeveExporRodadaQueAindaAguardaParticipantes() {
        organizacaoAutenticada();

        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = sumoCategory();

        for (int i = 0; i < 4; i++) {
            Team team = team();
            Registration registration = approvedRegistration(
                    competition,
                    category,
                    team,
                    robot(team));
            inspecionar(registration);
        }

        Long bracketId = bracketGenerationService.gerar(
                competition.getId(), category.getId()).getId();

        var partidas = matchRepository
                .findByBracketIdOrderByRodadaAscOrdemAsc(bracketId);
        long aguardando = partidas.stream()
                .filter(item -> item.getStatus()
                        == br.edu.ufrb.rascomp.model.Enum.StatusMatch.AGUARDANDO_PARTICIPANTES)
                .count();
        assertEquals(1, aguardando);

        var agenda = agendaService.listar(competition.getId());
        long batalhas = agenda.stream()
                .filter(item -> "SUMO_MATCH".equals(item.getTipo()))
                .count();

        assertEquals(2, batalhas);
        assertTrue(agenda.stream().noneMatch(item ->
                "SUMO_MATCH".equals(item.getTipo())
                        && partidas.stream()
                                .filter(match -> match.getStatus()
                                        == br.edu.ufrb.rascomp.model.Enum.StatusMatch.AGUARDANDO_PARTICIPANTES)
                                .anyMatch(match -> match.getId().equals(item.getMatchId()))));
    }

    @Test
    void desclassificacaoDeveSerAuditadaEResolverPartidaSemRoundFicticio() {
        organizacaoAutenticada();

        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = sumoCategory();

        Team teamA = team();
        Team teamB = team();
        Registration a = approvedRegistration(competition, category, teamA, robot(teamA));
        Registration b = approvedRegistration(competition, category, teamB, robot(teamB));

        inspecionar(a);
        inspecionar(b);

        Long bracketId = bracketGenerationService.gerar(
                competition.getId(), category.getId()).getId();
        Bracket bracket = bracketRepository.findById(bracketId).orElseThrow();
        Match match = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId())
                .stream()
                .filter(item -> item.getRegistrationA() != null && item.getRegistrationB() != null)
                .findFirst()
                .orElseThrow();

        competition.setStatus(StatusCompetition.EM_ANDAMENTO);
        competitionRepository.save(competition);

        MatchAgendaDTO agendaPartida = new MatchAgendaDTO();
        agendaPartida.setDataHora(LocalDateTime.now().plusHours(2));
        agendaPartida.setPista("Dohyo A");
        agendaPartida.setOrdemExecucao(2);
        agendaPartida.setStatusConvocacao(StatusConvocacaoPartida.PRONTA);
        matchService.atualizarAgenda(match.getId(), agendaPartida);

        var agendaAntes = agendaService.listar(competition.getId());
        assertTrue(agendaAntes.stream().anyMatch(item ->
                "SUMO_MATCH".equals(item.getTipo())
                        && item.getMatchId().equals(match.getId())
                        && "PRONTA".equals(item.getStatus())));

        long roundsAntes = roundSumoRepository.count();
        registrationService.desclassificar(a.getId(), "Infração técnica confirmada pela organização.");

        Registration desclassificada = registrationRepository.findById(a.getId()).orElseThrow();
        assertEquals(StatusRegistration.DESCLASSIFICADA, desclassificada.getStatus());

        var resultado = matchResultService.resolverIndisponibilidadeAdministrativa(match.getId());
        assertEquals(b.getId(), resultado.getWinnerRegistrationId());
        assertEquals(roundsAntes, roundSumoRepository.count());

        var agendaDepois = agendaService.listar(competition.getId());
        assertTrue(agendaDepois.stream().anyMatch(item ->
                "SUMO_MATCH".equals(item.getTipo())
                        && item.getMatchId().equals(match.getId())
                        && "FINALIZADA".equals(item.getStatus())));

        var historico = statusHistoryService.listar(a.getId());
        assertTrue(historico.stream().anyMatch(item ->
                item.getChangeType() == RegistrationStatusChangeType.DESCLASSIFICACAO
                        && item.getReason().contains("Infração técnica")));

        var vencedores = resultsService.listar(competition.getId());
        assertTrue(vencedores.stream().anyMatch(item ->
                item.getCategoryId().equals(category.getId())
                        && b.getId().equals(item.getWinnerRegistrationId())));
    }

    private void inspecionar(Registration registration) {
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal("0.450"));
        dto.setAprovada(true);
        dto.setObservacao("Apto no fluxo integrado.");
        inspecaoService.registrar(dto);
    }

    private TentativaSeguidorLinhaDTO tentativa(
            Registration registration,
            int tomada,
            int numero,
            String tempo) {
        TentativaSeguidorLinhaDTO dto = new TentativaSeguidorLinhaDTO();
        dto.setRegistrationId(registration.getId());
        dto.setTomada(tomada);
        dto.setNumeroTentativa(numero);
        dto.setTempoSegundos(new BigDecimal(tempo));
        dto.setCheckpointsAlcancados(5);
        dto.setPenalidadeSegundos(0);
        dto.setConcluida(true);
        dto.setValida(true);
        return dto;
    }
}
