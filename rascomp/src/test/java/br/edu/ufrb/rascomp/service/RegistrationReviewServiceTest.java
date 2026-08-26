package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.access.AccessDeniedException;

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
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationReviewServiceTest {

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
    private CompetitionCategory category;
    private Team team;
    private Robot robot;

    @BeforeEach
    void setup() {
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        team = new Team();
        team.setId(2L);
        team.setNome("Equipe Demo");
        team.setInstitution(institution);
        team.setAtivo(true);

        robot = new Robot();
        robot.setId(3L);
        robot.setNome("Titan");
        robot.setTeam(team);
        robot.setAtivo(true);

        category = CompetitionCategory.builder()
                .id(4L).nome("Mini Sumô").modalidade(Modalidade.SUMO).ativo(true).build();

        competition = new Competition();
        competition.setId(5L);
        competition.setNome("RRC Teste");
        competition.setInicioInscricoes(LocalDate.now().minusDays(10));
        competition.setFimInscricoes(LocalDate.now().plusDays(10));
        competition.setDataInicio(LocalDate.now().plusDays(20));
        competition.setDataFim(LocalDate.now().plusDays(21));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setAtivo(true);

        registration = new Registration();
        registration.setId(6L);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(new java.util.LinkedHashSet<>());
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);

        when(registrationRepository.findById(6L)).thenReturn(Optional.of(registration));
        when(competitionRepository.findById(5L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(robotRepository.findById(3L)).thenReturn(Optional.of(robot));
        when(registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotIdAndIdNot(5L, 4L, 3L, 6L))
                .thenReturn(false);
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void organizacaoDeveAprovarERegistrarRevisor() {
        UserAccount organizacao = user(90L, UserRole.ORGANIZACAO);
        when(userAccountService.buscarAtual()).thenReturn(organizacao);

        RegistrationDTO result = service.atualizar(6L, dto(StatusRegistration.APROVADA));

        assertEquals(StatusRegistration.APROVADA, result.getStatus());
        assertEquals(organizacao.getId(), registration.getReviewedByUser().getId());
        assertNotNull(registration.getReviewedAt());
    }

    @Test
    void participanteNaoPodeAprovarInscricao() {
        when(userAccountService.buscarAtual()).thenReturn(user(91L, UserRole.PARTICIPANTE));

        assertThrows(AccessDeniedException.class,
                () -> service.atualizar(6L, dto(StatusRegistration.APROVADA)));
    }

    @Test
    void rejeicaoTambemDeveRegistrarAuditoria() {
        UserAccount organizacao = user(92L, UserRole.ORGANIZACAO);
        when(userAccountService.buscarAtual()).thenReturn(organizacao);

        service.atualizar(6L, dto(StatusRegistration.REJEITADA));

        assertEquals(StatusRegistration.REJEITADA, registration.getStatus());
        assertEquals(92L, registration.getReviewedByUser().getId());
        assertNotNull(registration.getReviewedAt());
    }

    private RegistrationDTO dto(StatusRegistration status) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(5L);
        dto.setCategoryId(4L);
        dto.setTeamId(2L);
        dto.setRobotId(3L);
        dto.setCompetitorIds(List.of());
        dto.setStatus(status);
        dto.setAtivo(true);
        return dto;
    }

    private UserAccount user(Long id, UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNome(role.name());
        user.setEmail(role.name().toLowerCase() + id + "@rascomp.local");
        user.setRole(role);
        user.setAtivo(true);
        return user;
    }
}
