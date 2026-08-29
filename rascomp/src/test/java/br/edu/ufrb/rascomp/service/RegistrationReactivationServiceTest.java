package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationReactivationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private UserAccountService userAccountService;

    @InjectMocks private RegistrationService service;

    private Registration registration;
    private Competition competition;

    @BeforeEach
    void setup() {
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        Team team = new Team();
        team.setId(2L);
        team.setNome("Equipe Demo");
        team.setInstitution(institution);
        team.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(3L);
        robot.setNome("Titan");
        robot.setTeam(team);
        robot.setAtivo(true);

        CompetitionCategory category = CompetitionCategory.builder()
                .id(4L)
                .nome("Mini Sumô")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        competition = new Competition();
        competition.setId(5L);
        competition.setNome("RRC Teste");
        competition.setInicioInscricoes(LocalDate.now().minusDays(5));
        competition.setFimInscricoes(LocalDate.now().plusDays(5));
        competition.setDataInicio(LocalDate.now().plusDays(10));
        competition.setDataFim(LocalDate.now().plusDays(11));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setAtivo(true);

        UserAccount reviewer = new UserAccount();
        reviewer.setId(90L);

        registration = new Registration();
        registration.setId(6L);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(StatusRegistration.CANCELADA);
        registration.setAtivo(false);
        registration.setReviewedByUser(reviewer);
        registration.setReviewedAt(LocalDateTime.now().minusDays(1));

        when(registrationRepository.findById(6L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveReativarQuandoInscricoesEstaoAbertasEDentroDaJanela() {
        RegistrationDTO result = service.reativar(6L);

        assertTrue(result.getAtivo());
        assertEquals(StatusRegistration.PENDENTE, result.getStatus());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.PENDENTE, registration.getStatus());
        assertNull(registration.getReviewedByUser());
        assertNull(registration.getReviewedAt());
        verify(registrationRepository).save(registration);
    }

    @Test
    void deveBloquearReativacaoQuandoPeriodoDeInscricaoJaTerminou() {
        competition.setFimInscricoes(LocalDate.now().minusDays(1));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.reativar(6L));

        assertEquals("As inscrições não estão abertas para esta competição.", error.getMessage());
        assertFalse(registration.getAtivo());
        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void deveBloquearReativacaoQuandoStatusDaCompeticaoNaoPermiteInscricoes() {
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.reativar(6L));

        assertEquals("As inscrições não estão abertas para esta competição.", error.getMessage());
        assertFalse(registration.getAtivo());
        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }
}
