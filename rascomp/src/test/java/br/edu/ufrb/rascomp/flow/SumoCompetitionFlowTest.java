package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.RoundSumoService;

@SpringBootTest
@ActiveProfiles("flowtest")
class SumoCompetitionFlowTest extends IntegrationFlowTestSupport {

    @Autowired private InspecaoSumoService inspecaoService;
    @Autowired private BracketGenerationService bracketGenerationService;
    @Autowired private RoundSumoService roundSumoService;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void deveExecutarInspecaoChaveRoundsResultadoECampeao() {
        SumoFixture fixture = prepararPartida();
        Match match = fixture.match();
        Registration vencedor = match.getRegistrationA();

        roundSumoService.registrar(round(match, vencedor, 0, 0));
        roundSumoService.registrar(round(match, vencedor, 0, 0));

        Match persistida = matchRepository.findById(match.getId()).orElseThrow();
        Bracket bracket = bracketRepository.findById(fixture.bracket().getId()).orElseThrow();

        assertEquals(StatusMatch.FINALIZADA, persistida.getStatus());
        assertTrue(matchResultRepository.findByMatchId(match.getId()).isPresent());
        assertEquals(vencedor.getId(),
                matchResultRepository.findByMatchId(match.getId()).orElseThrow().getWinner().getId());
        assertEquals(StatusBracket.FINALIZADO, bracket.getStatus());
    }

    @Test
    void byeDeveAvancarSemSerTratadoComoDisputaReal() {
        organizacaoAutenticada();
        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = sumoCategory();

        for (int i = 0; i < 3; i++) {
            Team team = team();
            Registration registration = approvedRegistration(competition, category, team, robot(team));
            inspecionar(registration);
        }

        Long bracketId = bracketGenerationService.gerar(competition.getId(), category.getId()).getId();
        var matches = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId);

        Match bye = matches.stream()
                .filter(item -> item.getRodada() == 1)
                .filter(item -> (item.getRegistrationA() == null) ^ (item.getRegistrationB() == null))
                .findFirst()
                .orElseThrow();
        Match finalMatch = matches.stream()
                .filter(item -> item.getRodada() == 2)
                .findFirst()
                .orElseThrow();

        assertEquals(StatusMatch.FINALIZADA, matchRepository.findById(bye.getId()).orElseThrow().getStatus());
        Match finalPersistida = matchRepository.findById(finalMatch.getId()).orElseThrow();
        assertEquals(StatusMatch.AGUARDANDO_PARTICIPANTES, finalPersistida.getStatus());
        assertTrue((finalPersistida.getRegistrationA() != null) ^ (finalPersistida.getRegistrationB() != null));
        assertEquals(StatusBracket.GERADO, bracketRepository.findById(bracketId).orElseThrow().getStatus());
    }

    @Test
    void duasPenalidadesDevemDarVitoriaAutomaticaAoAdversario() {
        SumoFixture fixture = prepararPartida();
        Match match = fixture.match();

        RoundSumoDTO dto = round(match, null, 2, 0);
        RoundSumoDTO salvo = roundSumoService.registrar(dto);

        assertEquals(match.getRegistrationB().getId(), salvo.getWinnerRegistrationId());
        assertEquals(MotivoResultadoRoundSumo.PENALIDADES, salvo.getMotivoResultado());
        assertEquals(StatusRoundSumo.FINALIZADO, salvo.getStatus());
    }

    private SumoFixture prepararPartida() {
        organizacaoAutenticada();
        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = sumoCategory();

        Team teamA = team();
        Team teamB = team();
        Registration a = approvedRegistration(competition, category, teamA, robot(teamA));
        Registration b = approvedRegistration(competition, category, teamB, robot(teamB));

        inspecionar(a);
        inspecionar(b);

        Long bracketId = bracketGenerationService.gerar(competition.getId(), category.getId()).getId();
        Bracket bracket = bracketRepository.findById(bracketId).orElseThrow();
        Match match = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId).stream()
                .filter(item -> item.getRegistrationA() != null && item.getRegistrationB() != null)
                .findFirst()
                .orElseThrow();

        return new SumoFixture(bracket, match);
    }

    private void inspecionar(Registration registration) {
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal("0.510"));
        dto.setAprovada(true);
        dto.setObservacao("APTO por decisao humana no fluxo integrado.");
        inspecaoService.registrar(dto);
    }

    private RoundSumoDTO round(
            Match match,
            Registration winner,
            int penalidadesA,
            int penalidadesB) {
        RoundSumoDTO dto = new RoundSumoDTO();
        dto.setMatchId(match.getId());
        dto.setWinnerRegistrationId(winner == null ? null : winner.getId());
        dto.setStatus(StatusRoundSumo.FINALIZADO);
        dto.setMotivoResultado(MotivoResultadoRoundSumo.DISPUTA);
        dto.setPenalidadesA(penalidadesA);
        dto.setPenalidadesB(penalidadesB);
        return dto;
    }

    private record SumoFixture(Bracket bracket, Match match) {}
}
