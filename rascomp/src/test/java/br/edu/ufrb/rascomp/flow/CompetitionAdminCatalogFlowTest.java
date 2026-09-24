package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.service.CompetitionAdminCatalogService;

@SpringBootTest
@ActiveProfiles("flowtest")
class CompetitionAdminCatalogFlowTest extends IntegrationFlowTestSupport {

    @Autowired
    private CompetitionAdminCatalogService catalogService;

    @Autowired
    private CompetitorRepository competitorRepository;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void deveMontarCatalogoComRelacoesLazyDentroDaTransacao() {
        organizacaoAutenticada();

        var competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        var category = followCategory();
        var team = team();
        var robot = robot(team);

        Competitor competitor = new Competitor();
        competitor.setNome(unique("Competidor"));
        competitor.setEmail(unique("competidor").toLowerCase() + "@flow.local");
        competitor.setTeam(team);
        competitor.setAtivo(true);
        competitor = competitorRepository.save(competitor);

        var registration = approvedRegistration(competition, category, team, robot);
        registration.setCompetitors(new LinkedHashSet<>(java.util.List.of(competitor)));
        registrationRepository.save(registration);

        var result = catalogService.buscar(competition.getId());

        assertEquals(1, result.getTeams().size());
        assertEquals(team.getInstitution().getNome(), result.getTeams().get(0).getInstitutionNome());

        assertEquals(1, result.getRobots().size());
        assertEquals(team.getNome(), result.getRobots().get(0).getTeamNome());

        assertEquals(1, result.getCompetitors().size());
        assertEquals(team.getNome(), result.getCompetitors().get(0).getTeamNome());
    }
}
