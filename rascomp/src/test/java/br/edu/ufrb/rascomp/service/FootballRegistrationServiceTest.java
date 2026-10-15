package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.*;
import br.edu.ufrb.rascomp.model.Enum.*;
import br.edu.ufrb.rascomp.repository.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FootballRegistrationServiceTest {
    @Mock RegistrationRepository registrationRepository;
    @Mock CompetitionContextService competitionContextService;
    @Mock CompetitionRepository competitionRepository;
    @Mock CompetitionCategoryRepository categoryRepository;
    @Mock CompetitionCategoryAssignmentService categoryAssignments;
    @Mock TeamRepository teamRepository;
    @Mock RobotRepository robotRepository;
    @Mock RobotResponsibleRepository robotResponsibleRepository;
    @Mock CompetitorRepository competitorRepository;
    @Mock UserAccountService userAccountService;
    @Mock TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock AusenciaTomadaSeguidorLinhaRepository ausenciaFollowRepository;
    @Mock InspecaoSumoRepository inspecaoSumoRepository;
    @Mock MatchRepository matchRepository;
    @Mock RegistrationStatusHistoryService statusHistoryService;
    @Mock ParticipantCompetitionRegistrationService participantCompetitionRegistrationService;
    @Mock RegistrationReceiptStorageService receiptStorageService;
    @Mock RegistrationCompositionService compositionService;
    @Mock RegistrationLotService registrationLotService;
    @InjectMocks RegistrationService service;

    Competition competition;
    CompetitionCategory category;
    Team team;
    Competitor competitor;
    UserAccount participant;
    UserAccount manager;

    @BeforeEach void setup() {
        Institution institution = new Institution();
        institution.setId(7L);
        institution.setAtivo(true);
        team = new Team();
        team.setId(2L);
        team.setAtivo(true);
        team.setInstitution(institution);
        participant = new UserAccount();
        participant.setId(9L);
        participant.setRole(UserRole.PARTICIPANTE);
        manager = new UserAccount();
        manager.setId(8L);
        manager.setRole(UserRole.GESTAO);
        competitor = new Competitor();
        competitor.setId(3L);
        competitor.setAtivo(true);
        competitor.setTeam(team);
        competitor.setUserAccount(participant);
        competition = new Competition();
        competition.setId(1L);
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setInicioInscricoes(LocalDate.now().minusDays(2));
        competition.setFimInscricoes(LocalDate.now().plusDays(2));
        category = CompetitionCategory.builder().id(4L).nome("Futebol").modalidade(Modalidade.FUTEBOL).ativo(true).build();
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(competitorRepository.findByUserAccountId(9L)).thenReturn(Optional.of(competitor));
        when(competitorRepository.findById(3L)).thenReturn(Optional.of(competitor));
        when(registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(1L)).thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });
    }

    RegistrationDTO payload() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(1L);
        dto.setCategoryId(4L);
        dto.setTeamId(2L);
        return dto;
    }

    @Test void participanteInscreveFutebolSemRobo() {
        RegistrationDTO result = service.criarPorParticipante(payload(), participant);
        assertEquals(StatusRegistration.PENDENTE, result.getStatus());
        assertNull(result.getRobotId());
        assertEquals(List.of(3L), result.getCompetitorIds());
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test void gestaoAprovaFutebolSemRoboComParticipanteAprovado() {
        Registration registration = new Registration();
        registration.setId(20L);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(null);
        registration.setCompetitors(Set.of(competitor));
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);
        registration.setPaymentReceiptStorageKey("receipt-test");
        when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
        when(userAccountService.buscarAtual()).thenReturn(manager);
        ParticipantCompetitionRegistration personal = new ParticipantCompetitionRegistration();
        personal.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);
        when(participantCompetitionRegistrationService.findByCompetitionAndCompetitor(1L,3L)).thenReturn(personal);

        RegistrationDTO request = payload();
        request.setCompetitorIds(List.of(3L));
        request.setStatus(StatusRegistration.APROVADA);
        RegistrationDTO result = service.atualizar(20L, request);
        assertEquals(StatusRegistration.APROVADA, result.getStatus());
        assertNull(result.getRobotId());
        assertEquals(8L, result.getReviewedByUserId());
    }

    @Test void futebolNaoDispensaRoboNasOutrasModalidades() {
        category.setModalidade(Modalidade.FOLLOW_LINE);
        assertThrows(IllegalArgumentException.class, () -> service.criarPorParticipante(payload(), participant));
    }
}
