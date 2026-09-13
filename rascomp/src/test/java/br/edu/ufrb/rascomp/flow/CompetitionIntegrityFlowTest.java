package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.BatalhaSumoDTO;
import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoItemDTO;
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
class CompetitionIntegrityFlowTest extends IntegrationFlowTestSupport {

    @Autowired private BracketGenerationService bracketGenerationService;
    @Autowired private InspecaoSumoService inspecaoService;
    @Autowired private RoundSumoService roundSumoService;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void falhaAoGerarChaveEmEstadoInvalidoNaoPodePersistirEstruturaParcial() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        CompetitionCategory category = sumoCategory();

        long bracketsAntes = bracketRepository.count();
        long matchesAntes = matchRepository.count();

        assertThrows(IllegalArgumentException.class,
                () -> bracketGenerationService.gerar(competition.getId(), category.getId()));

        assertEquals(bracketsAntes, bracketRepository.count());
        assertEquals(matchesAntes, matchRepository.count());
    }

    @Test
    void batalhaComSegundoRoundInvalidoDeveFazerRollbackDoPrimeiroRound() {
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

        long roundsAntes = roundSumoRepository.count();
        long resultadosAntes = matchResultRepository.count();

        BatalhaSumoDTO batalha = new BatalhaSumoDTO();
        batalha.setMatchId(match.getId());
        batalha.setRounds(List.of(
                item(match.getRegistrationA().getId(), 0, 0),
                item(match.getRegistrationA().getId(), 2, 2)));

        assertThrows(IllegalArgumentException.class, () -> roundSumoService.registrarBatalha(batalha));

        assertEquals(roundsAntes, roundSumoRepository.count());
        assertEquals(resultadosAntes, matchResultRepository.count());

        Match persistida = matchRepository.findById(match.getId()).orElseThrow();
        Bracket persistido = bracketRepository.findById(bracket.getId()).orElseThrow();
        assertEquals(StatusMatch.AGENDADA, persistida.getStatus());
        assertEquals(StatusBracket.GERADO, persistido.getStatus());
        assertFalse(matchResultRepository.findByMatchId(match.getId()).isPresent());
    }

    private void inspecionar(Registration registration) {
        InspecaoSumoDTO dto = new InspecaoSumoDTO();
        dto.setRegistrationId(registration.getId());
        dto.setPesoMedido(new BigDecimal("0.490"));
        dto.setAprovada(true);
        inspecaoService.registrar(dto);
    }

    private RoundSumoItemDTO item(Long winnerId, int penalidadesA, int penalidadesB) {
        RoundSumoItemDTO dto = new RoundSumoItemDTO();
        dto.setWinnerRegistrationId(winnerId);
        dto.setStatus(StatusRoundSumo.FINALIZADO);
        dto.setMotivoResultado(MotivoResultadoRoundSumo.DISPUTA);
        dto.setPenalidadesA(penalidadesA);
        dto.setPenalidadesB(penalidadesB);
        return dto;
    }
}
