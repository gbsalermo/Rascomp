package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class RankingFollowServiceTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;

    @InjectMocks
    private RankingFollowService service;

    private CompetitionCategory categoriaFollow;

    @BeforeEach
    void setUp() {
        categoriaFollow = CompetitionCategory.builder()
                .id(3L)
                .nome("Seguidor de Linha")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();
    }

    @Test
    void deveEscolherMelhorTentativaDaTomadaConsiderandoPenalidade() {
        stubCompetitionAndCategory(categoriaFollow);
        Registration registration = registrationBase();

        TentativaSeguidorLinha tentativa1 = tentativa(new BigDecimal("42.315"), 0, 1, 1);
        TentativaSeguidorLinha tentativa2 = tentativa(new BigDecimal("40.870"), 2, 1, 2);

        when(registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(1L))
                .thenReturn(List.of(registration));
        when(tentativaRepository.findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(1L))
                .thenReturn(List.of(tentativa1, tentativa2));

        List<RankingFollowDTO> ranking = service.gerarRanking(1L, 3L);

        assertEquals(1, ranking.size());
        assertEquals(1, ranking.get(0).getPosicao());
        assertEquals(1, ranking.get(0).getTomada());
        assertEquals(1, ranking.get(0).getNumeroTentativa());
        assertEquals(0, new BigDecimal("42.315").compareTo(ranking.get(0).getTempoFinalSegundos()));
    }

    @Test
    void deveEscolherMelhorTomadaDoRoboDepoisDeApurarCadaTomada() {
        stubCompetitionAndCategory(categoriaFollow);
        Registration registration = registrationBase();

        TentativaSeguidorLinha tomada1Tentativa1 = tentativa(new BigDecimal("44.000"), 0, 1, 1);
        TentativaSeguidorLinha tomada1Tentativa2 = tentativa(new BigDecimal("41.000"), 4, 1, 2);
        TentativaSeguidorLinha tomada2Tentativa1 = tentativa(new BigDecimal("42.000"), 0, 2, 1);
        TentativaSeguidorLinha tomada2Tentativa2 = tentativa(new BigDecimal("40.500"), 0, 2, 2);

        when(registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(1L))
                .thenReturn(List.of(registration));
        when(tentativaRepository.findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(1L))
                .thenReturn(List.of(
                        tomada1Tentativa1,
                        tomada1Tentativa2,
                        tomada2Tentativa1,
                        tomada2Tentativa2));

        List<RankingFollowDTO> ranking = service.gerarRanking(1L, 3L);

        assertEquals(1, ranking.size());
        assertEquals(2, ranking.get(0).getTomada());
        assertEquals(2, ranking.get(0).getNumeroTentativa());
        assertEquals(0, new BigDecimal("40.500").compareTo(ranking.get(0).getTempoFinalSegundos()));
    }

    @Test
    void deveRejeitarRankingParaCategoriaSumo() {
        CompetitionCategory categoriaSumo = CompetitionCategory.builder()
                .id(3L)
                .nome("Mini Sumo")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();
        stubCompetitionAndCategory(categoriaSumo);

        assertThrows(IllegalArgumentException.class, () -> service.gerarRanking(1L, 3L));
    }

    private Registration registrationBase() {
        Registration registration = mock(Registration.class);
        Robot robot = mock(Robot.class);
        Team team = mock(Team.class);

        when(registration.getId()).thenReturn(1L);
        when(registration.getCategory()).thenReturn(categoriaFollow);
        when(registration.getAtivo()).thenReturn(true);
        when(registration.getStatus()).thenReturn(StatusRegistration.APROVADA);
        when(registration.getRobot()).thenReturn(robot);
        when(registration.getTeam()).thenReturn(team);
        when(robot.getId()).thenReturn(1L);
        when(robot.getNome()).thenReturn("Vespa");
        when(team.getNome()).thenReturn("RAS UFRB");
        return registration;
    }

    private void stubCompetitionAndCategory(CompetitionCategory category) {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(mock(Competition.class)));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
    }

    private TentativaSeguidorLinha tentativa(
            BigDecimal tempo,
            Integer penalidade,
            Integer tomada,
            Integer numeroTentativa) {

        TentativaSeguidorLinha tentativa = new TentativaSeguidorLinha();
        tentativa.setValida(true);
        tentativa.setConcluida(true);
        tentativa.setTempoSegundos(tempo);
        tentativa.setPenalidadeSegundos(penalidade);
        tentativa.setTomada(tomada);
        tentativa.setNumeroTentativa(numeroTentativa);
        return tentativa;
    }
}
