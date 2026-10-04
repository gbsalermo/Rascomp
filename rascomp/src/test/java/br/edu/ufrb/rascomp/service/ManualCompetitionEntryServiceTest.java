package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ManualCompetitionEntryRequest;
import br.edu.ufrb.rascomp.dto.ManualParticipantEntryRequest;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationDTO;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ManualCompetitionEntryServiceTest {

    @Mock private UserAccountService userAccountService;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private CompetitorService competitorService;
    @Mock private ParticipantCompetitionRegistrationService participantCompetitionRegistrationService;
    @Mock private RobotService robotService;
    @Mock private RobotResponsibleService robotResponsibleService;
    @Mock private RegistrationService registrationService;

    @InjectMocks
    private ManualCompetitionEntryService service;

    private UserAccount dev;
    private UserAccount participant;
    private Team team;

    @BeforeEach
    void setUp() {
        dev = new UserAccount();
        dev.setId(1L);
        dev.setNome("DEV");
        dev.setRole(UserRole.DEV);
        dev.setAtivo(true);

        participant = new UserAccount();
        participant.setId(2L);
        participant.setNome("Participante");
        participant.setEmail("participante@rascomp.local");
        participant.setTelefone("75999999999");
        participant.setRole(UserRole.PARTICIPANTE);
        participant.setAtivo(true);

        Institution institution = new Institution();
        institution.setId(3L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        team = new Team();
        team.setId(4L);
        team.setNome("Equipe Teste");
        team.setInstitution(institution);
        team.setAtivo(true);
    }

    @Test
    void devPodeAdicionarContaNovaSemEquipeComoParticipanteManual() {
        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(competitorRepository.findByUserAccountId(2L))
                .thenReturn(Optional.empty(), Optional.of(competitor()));
        when(teamRepository.findById(4L)).thenReturn(Optional.of(team));
        when(competitorService.criar(any(CompetitorDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantCompetitionRegistrationDTO approved =
                new ParticipantCompetitionRegistrationDTO();
        approved.setCompetitionId(10L);
        approved.setCompetitorId(30L);
        approved.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);

        when(participantCompetitionRegistrationService.criarEntradaManualDev(
                eq(10L),
                any(Competitor.class),
                eq(dev),
                eq("Inclusão autorizada")))
                .thenReturn(approved);

        ParticipantCompetitionRegistrationDTO result =
                service.criarParticipante(participantRequest());

        assertEquals(
                ParticipantCompetitionRegistrationStatus.APROVADA,
                result.getStatus());

        ArgumentCaptor<CompetitorDTO> captor =
                ArgumentCaptor.forClass(CompetitorDTO.class);
        verify(competitorService).criar(captor.capture());
        assertEquals(4L, captor.getValue().getTeamId());
        assertEquals(2L, captor.getValue().getUserAccountId());
    }

    @Test
    void devPodeCriarRoboEInscricaoManualParaContaNovaEscolhendoEquipe() {
        Competitor competitor = competitor();

        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(competitorRepository.findByUserAccountId(2L))
                .thenReturn(Optional.empty(), Optional.of(competitor));
        when(teamRepository.findById(4L)).thenReturn(Optional.of(team));
        when(competitorService.criar(any(CompetitorDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantCompetitionRegistrationDTO personal =
                new ParticipantCompetitionRegistrationDTO();
        personal.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);
        when(participantCompetitionRegistrationService.criarEntradaManualDev(
                10L, competitor, dev, "Inclusão autorizada"))
                .thenReturn(personal);

        RobotDTO robot = new RobotDTO();
        robot.setId(40L);
        robot.setNome("Avulso");
        robot.setTeamId(4L);
        when(robotService.criar(any(RobotDTO.class), eq(participant)))
                .thenReturn(robot);

        RegistrationDTO saved = new RegistrationDTO();
        saved.setId(50L);
        saved.setRobotId(40L);
        saved.setRobotNome("Avulso");
        saved.setStatus(StatusRegistration.APROVADA);
        when(registrationService.criarEntradaManualDev(
                any(RegistrationDTO.class),
                eq(dev),
                eq("Inclusão autorizada")))
                .thenReturn(saved);

        RegistrationDTO result = service.criar(robotRequest());

        assertEquals(50L, result.getId());
        assertEquals(StatusRegistration.APROVADA, result.getStatus());

        verify(robotResponsibleService)
                .associarManual(40L, 30L, dev);

        ArgumentCaptor<RegistrationDTO> captor =
                ArgumentCaptor.forClass(RegistrationDTO.class);
        verify(registrationService).criarEntradaManualDev(
                captor.capture(),
                eq(dev),
                eq("Inclusão autorizada"));

        assertEquals(10L, captor.getValue().getCompetitionId());
        assertEquals(20L, captor.getValue().getCategoryId());
        assertEquals(4L, captor.getValue().getTeamId());
        assertEquals(java.util.List.of(30L), captor.getValue().getCompetitorIds());
    }

    @Test
    void naoDeveTransferirSilenciosamenteParticipanteJaVinculado() {
        Competitor competitor = competitor();
        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(competitorRepository.findByUserAccountId(2L))
                .thenReturn(Optional.of(competitor));

        ManualParticipantEntryRequest request = participantRequest();
        request.setTeamId(99L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarParticipante(request));

        assertEquals(true, error.getMessage().contains("já está associado"));
    }

    @Test
    void gestaoNaoPodeUsarEntradaManualDev() {
        UserAccount gestao = new UserAccount();
        gestao.setRole(UserRole.GESTAO);
        gestao.setAtivo(true);
        when(userAccountService.buscarAtual()).thenReturn(gestao);

        assertThrows(
                AccessDeniedException.class,
                () -> service.criarParticipante(participantRequest()));

        verifyNoInteractions(
                userAccountRepository,
                competitorRepository,
                teamRepository,
                competitorService,
                participantCompetitionRegistrationService);
    }

    private Competitor competitor() {
        Competitor competitor = new Competitor();
        competitor.setId(30L);
        competitor.setNome(participant.getNome());
        competitor.setEmail(participant.getEmail());
        competitor.setTeam(team);
        competitor.setUserAccount(participant);
        competitor.setAtivo(true);
        return competitor;
    }

    private ManualParticipantEntryRequest participantRequest() {
        ManualParticipantEntryRequest request =
                new ManualParticipantEntryRequest();
        request.setCompetitionId(10L);
        request.setParticipantUserId(2L);
        request.setTeamId(4L);
        request.setJustificativa("Inclusão autorizada");
        return request;
    }

    private ManualCompetitionEntryRequest robotRequest() {
        ManualCompetitionEntryRequest request =
                new ManualCompetitionEntryRequest();
        request.setCompetitionId(10L);
        request.setCategoryId(20L);
        request.setParticipantUserId(2L);
        request.setTeamId(4L);
        request.setRobotNome("Avulso");
        request.setRobotDescricao("Entrada tardia aprovada pela organização.");
        request.setJustificativa("Inclusão autorizada");
        return request;
    }
}
