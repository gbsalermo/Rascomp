package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
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
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.SumoPhysicalClass;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.InspecaoSumoRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationHybridRobotServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private UserAccountService userAccountService;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private InspecaoSumoRepository inspecaoSumoRepository;
    @Mock private MatchRepository matchRepository;

    @InjectMocks
    private RegistrationService service;

    private Competition competition;
    private Team team;
    private Robot robot;
    private CompetitionCategory miniAuto;
    private CompetitionCategory miniRc;
    private CompetitionCategory sumo3kg;
    private CompetitionCategory follow;

    @BeforeEach
    void setup() {
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        team = new Team();
        team.setId(2L);
        team.setNome("Equipe");
        team.setInstitution(institution);
        team.setAtivo(true);

        robot = new Robot();
        robot.setId(3L);
        robot.setNome("Titã");
        robot.setTeam(team);
        robot.setAtivo(true);

        competition = new Competition();
        competition.setId(4L);
        competition.setNome("RRC");
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setInicioInscricoes(LocalDate.now().minusDays(1));
        competition.setFimInscricoes(LocalDate.now().plusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(2));
        competition.setDataFim(LocalDate.now().plusDays(3));

        miniAuto = categoria(10L, "Mini Sumô Auto", Modalidade.SUMO, SumoPhysicalClass.MINI_500G);
        miniRc = categoria(11L, "Mini Sumô R/C", Modalidade.SUMO, SumoPhysicalClass.MINI_500G);
        sumo3kg = categoria(12L, "Sumô 3 kg R/C", Modalidade.SUMO, SumoPhysicalClass.SUMO_3KG);
        follow = categoria(13L, "Seguidor de Linha", Modalidade.FOLLOW_LINE, null);

        when(competitionRepository.findById(4L)).thenReturn(Optional.of(competition));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(robotRepository.findById(3L)).thenReturn(Optional.of(robot));
    }

    @Test
    void mesmoRoboPodeParticiparDeDuasCategoriasSumoDaMesmaClasseFisica() {
        Registration existente = registration(50L, miniAuto, StatusRegistration.APROVADA);
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(miniRc));
        when(registrationRepository.findByCompetitionIdAndRobotIdAndStatusIn(
                4L, 3L, List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(existente));
        when(registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(4L, 11L, 3L)).thenReturn(false);
        stubSave();

        RegistrationDTO result = service.criar(dto(11L));

        assertEquals(StatusRegistration.PENDENTE, result.getStatus());
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void mesmoRoboNaoPodeParticiparDeMiniE3kgNaMesmaEdicao() {
        Registration existente = registration(50L, miniAuto, StatusRegistration.APROVADA);
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(sumo3kg));
        when(registrationRepository.findByCompetitionIdAndRobotIdAndStatusIn(
                4L, 3L, List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.criar(dto(12L)));

        assertEquals(true, ex.getMessage().contains("MINI_500G"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void inscricaoFollowNaoConflitaComClasseFisicaDeSumo() {
        Registration existenteFollow = registration(51L, follow, StatusRegistration.APROVADA);
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(sumo3kg));
        when(registrationRepository.findByCompetitionIdAndRobotIdAndStatusIn(
                4L, 3L, List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(existenteFollow));
        when(registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(4L, 12L, 3L)).thenReturn(false);
        stubSave();

        RegistrationDTO result = service.criar(dto(12L));

        assertEquals(StatusRegistration.PENDENTE, result.getStatus());
    }

    @Test
    void categoriaSumoSemClasseFisicaNaoPodeReceberNovaInscricao() {
        CompetitionCategory legacy = categoria(14L, "Sumô legado", Modalidade.SUMO, null);
        when(categoryRepository.findById(14L)).thenReturn(Optional.of(legacy));

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto(14L)));

        verify(registrationRepository, never()).save(any());
    }

    private void stubSave() {
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration entity = invocation.getArgument(0);
            if (entity.getId() == null) entity.setId(100L);
            return entity;
        });
    }

    private CompetitionCategory categoria(Long id, String nome, Modalidade modalidade, SumoPhysicalClass physicalClass) {
        return CompetitionCategory.builder()
                .id(id)
                .nome(nome)
                .modalidade(modalidade)
                .sumoPhysicalClass(physicalClass)
                .ativo(true)
                .build();
    }

    private Registration registration(Long id, CompetitionCategory category, StatusRegistration status) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(status);
        registration.setAtivo(true);
        return registration;
    }

    private RegistrationDTO dto(Long categoryId) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(4L);
        dto.setCategoryId(categoryId);
        dto.setTeamId(2L);
        dto.setRobotId(3L);
        return dto;
    }
}
