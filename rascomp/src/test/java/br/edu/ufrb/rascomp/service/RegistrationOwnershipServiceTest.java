package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationOwnershipServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private RegistrationService service;

    @Test
    void participanteDeveCriarInscricaoPendenteComSolicitanteECompetidores() {
        Cenário c = cenario();
        RegistrationDTO dto = dto(c.team.getId(), c.robot.getId(), c.competitor.getId());

        prepararRepositorios(c);
        when(registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(1L, 2L, 4L))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        RegistrationDTO resultado = service.criarPorParticipante(dto, c.user);

        assertEquals(100L, resultado.getId());
        assertEquals(StatusRegistration.PENDENTE, resultado.getStatus());
        assertEquals(c.user.getId(), resultado.getRequestedByUserId());
        assertEquals(List.of(c.competitor.getId()), resultado.getCompetitorIds());
    }

    @Test
    void participanteNaoPodeInscreverCompetidorDeOutraEquipe() {
        Cenário c = cenario();
        Team outraEquipe = new Team();
        outraEquipe.setId(99L);
        outraEquipe.setAtivo(true);
        outraEquipe.setInstitution(c.institution);
        c.competitor.setTeam(outraEquipe);

        RegistrationDTO dto = dto(c.team.getId(), c.robot.getId(), c.competitor.getId());
        prepararRepositorios(c);
        when(registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(1L, 2L, 4L))
                .thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarPorParticipante(dto, c.user));

        assertTrue(ex.getMessage().contains("equipe informada"));
    }

    private void prepararRepositorios(Cenário c) {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(c.competition));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(c.category));
        when(teamRepository.findById(3L)).thenReturn(Optional.of(c.team));
        when(robotRepository.findById(4L)).thenReturn(Optional.of(c.robot));
        when(competitorRepository.findById(5L)).thenReturn(Optional.of(c.competitor));
    }

    private RegistrationDTO dto(Long teamId, Long robotId, Long competitorId) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(1L);
        dto.setCategoryId(2L);
        dto.setTeamId(teamId);
        dto.setRobotId(robotId);
        dto.setCompetitorIds(List.of(competitorId));
        return dto;
    }

    private Cenário cenario() {
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setNome("Responsável");
        user.setEmail("responsavel@teste.com");
        user.setRole(UserRole.PARTICIPANTE);
        user.setAtivo(true);

        Team team = new Team();
        team.setId(3L);
        team.setNome("Equipe A");
        team.setInstitution(institution);
        team.setResponsibleUser(user);
        team.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(4L);
        robot.setNome("Robô A");
        robot.setTeam(team);
        robot.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(5L);
        competitor.setNome("Competidor A");
        competitor.setEmail("competidor@teste.com");
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC Teste");
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setInicioInscricoes(LocalDate.now().minusDays(1));
        competition.setFimInscricoes(LocalDate.now().plusDays(1));

        CompetitionCategory category = CompetitionCategory.builder()
                .id(2L)
                .nome("Seguidor")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        return new Cenário(institution, user, team, robot, competitor, competition, category);
    }

    private record Cenário(
            Institution institution,
            UserAccount user,
            Team team,
            Robot robot,
            Competitor competitor,
            Competition competition,
            CompetitionCategory category) {}
}
