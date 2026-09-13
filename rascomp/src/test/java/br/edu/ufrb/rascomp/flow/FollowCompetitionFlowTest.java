package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.AusenciaTomadaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.service.AusenciaTomadaSeguidorLinhaService;
import br.edu.ufrb.rascomp.service.RankingFollowService;
import br.edu.ufrb.rascomp.service.TentativaSeguidorLinhaService;

@SpringBootTest
@ActiveProfiles("flowtest")
class FollowCompetitionFlowTest extends IntegrationFlowTestSupport {

    @Autowired private TentativaSeguidorLinhaService tentativaService;
    @Autowired private AusenciaTomadaSeguidorLinhaService ausenciaService;
    @Autowired private RankingFollowService rankingService;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void deveClassificarMelhorTentativaETravarTomadaPerdidaPorAusencia() {
        Competition competition = competition(StatusCompetition.EM_ANDAMENTO);
        CompetitionCategory category = followCategory();
        Team team = team();
        Registration registration = approvedRegistration(competition, category, team, robot(team));

        tentativaService.criar(tentativa(registration, 1, 1, "40.000", 5, true, true));
        TentativaSeguidorLinhaDTO segunda = tentativa(registration, 1, 2, "42.000", 0, true, true);
        tentativaService.criar(segunda);

        organizacaoAutenticada();
        AusenciaTomadaSeguidorLinhaDTO ausencia = new AusenciaTomadaSeguidorLinhaDTO();
        ausencia.setRegistrationId(registration.getId());
        ausencia.setTomada(2);
        ausencia.setObservacao("Equipe nao compareceu a segunda tomada.");
        ausenciaService.marcar(ausencia);

        long antes = tentativaRepository.count();
        assertThrows(IllegalArgumentException.class,
                () -> tentativaService.criar(tentativa(registration, 2, 1, "39.000", 0, true, true)));
        assertEquals(antes, tentativaRepository.count());

        var ranking = rankingService.gerarRanking(competition.getId(), category.getId());
        assertEquals(1, ranking.size());
        assertEquals(registration.getId(), ranking.get(0).getRegistrationId());
        assertEquals(new BigDecimal("42.000"), ranking.get(0).getTempoFinalSegundos());
        assertEquals(1, ranking.get(0).getTomada());
        assertEquals(2, ranking.get(0).getNumeroTentativa());
    }

    @Test
    void tentativaComEstadoImpossivelNaoPodeSerPersistida() {
        Competition competition = competition(StatusCompetition.EM_ANDAMENTO);
        CompetitionCategory category = followCategory();
        Team team = team();
        Registration registration = approvedRegistration(competition, category, team, robot(team));

        long antes = tentativaRepository.count();
        TentativaSeguidorLinhaDTO invalida = tentativa(registration, 1, 1, "12.000", 0, false, true);

        assertThrows(IllegalArgumentException.class, () -> tentativaService.criar(invalida));
        assertEquals(antes, tentativaRepository.count());
    }

    private TentativaSeguidorLinhaDTO tentativa(
            Registration registration,
            int tomada,
            int numero,
            String tempo,
            int penalidade,
            boolean concluida,
            boolean valida) {
        TentativaSeguidorLinhaDTO dto = new TentativaSeguidorLinhaDTO();
        dto.setRegistrationId(registration.getId());
        dto.setTomada(tomada);
        dto.setNumeroTentativa(numero);
        dto.setTempoSegundos(tempo == null ? null : new BigDecimal(tempo));
        dto.setCheckpointsAlcancados(concluida ? 5 : 0);
        dto.setPenalidadeSegundos(penalidade);
        dto.setConcluida(concluida);
        dto.setValida(valida);
        return dto;
    }
}
