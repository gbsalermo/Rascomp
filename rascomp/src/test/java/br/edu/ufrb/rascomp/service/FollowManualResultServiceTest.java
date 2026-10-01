package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.FollowManualResultDTO;
import br.edu.ufrb.rascomp.dto.FollowManualResultRequest;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.FollowManualResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class FollowManualResultServiceTest {

    @Mock private FollowManualResultRepository repository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private FollowResolutionService resolutionService;
    @Mock private CompetitionContextService competitionContextService;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private FollowManualResultService service;

    private Competition competition;
    private CompetitionCategory category;
    private Registration first;
    private Registration second;
    private Registration third;

    @BeforeEach
    void setUp() {
        competition = new Competition();
        competition.setId(10L);

        category = new CompetitionCategory();
        category.setId(20L);
        category.setNome("Follow");
        category.setModalidade(Modalidade.FOLLOW_LINE);
        category.setAtivo(true);

        first = registration(1L, "Alpha");
        second = registration(2L, "Beta");
        third = registration(3L, "Gamma");

        UserAccount dev = new UserAccount();
        dev.setId(99L);
        dev.setNome("DEV");
        dev.setRole(UserRole.DEV);
        dev.setAtivo(true);

        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(competitionRepository.findById(10L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        10L, 20L, StatusRegistration.APROVADA))
                .thenReturn(List.of(first, second, third));
        lenient().when(registrationRepository.findById(1L)).thenReturn(Optional.of(first));
        lenient().when(registrationRepository.findById(2L)).thenReturn(Optional.of(second));
        lenient().when(registrationRepository.findById(3L)).thenReturn(Optional.of(third));
    }

    @Test
    void decisaoAdministrativaDeveRegistrarPodioOrdenadoCompleto() {
        when(repository.save(any(FollowManualResult.class))).thenAnswer(invocation -> {
            FollowManualResult result = invocation.getArgument(0);
            result.setId(50L);
            return result;
        });

        FollowManualResultDTO result = service.definir(request(1L, 2L, 3L));

        assertEquals(1L, result.getWinnerRegistrationId());
        assertEquals("Alpha", result.getWinnerRobotNome());
        assertEquals(2L, result.getSecondRegistrationId());
        assertEquals("Beta", result.getSecondRobotNome());
        assertEquals(3L, result.getThirdRegistrationId());
        assertEquals("Gamma", result.getThirdRobotNome());
    }

    @Test
    void podioAdministrativoNaoPodeRepetirInscricao() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.definir(request(1L, 1L, 3L)));
    }

    private FollowManualResultRequest request(Long firstId, Long secondId, Long thirdId) {
        FollowManualResultRequest request = new FollowManualResultRequest();
        request.setCompetitionId(10L);
        request.setCategoryId(20L);
        request.setWinnerRegistrationId(firstId);
        request.setSecondRegistrationId(secondId);
        request.setThirdRegistrationId(thirdId);
        request.setJustificativa("Decisão administrativa auditada.");
        return request;
    }

    private Registration registration(Long id, String robotName) {
        Team team = new Team();
        team.setId(id + 100);
        team.setNome("Equipe " + robotName);

        Robot robot = new Robot();
        robot.setId(id + 200);
        robot.setNome(robotName);
        robot.setTeam(team);

        Registration registration = new Registration();
        registration.setId(id);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);
        return registration;
    }
}
