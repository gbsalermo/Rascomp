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

import br.edu.ufrb.rascomp.dto.ManualCompetitionEntryRequest;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ManualCompetitionEntryServiceTest {

    @Mock private UserAccountService userAccountService;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private RobotService robotService;
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
    void devPodeCriarRoboEInscricaoManualParaCompetidorJaAssociado() {
        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(participant));

        Competitor competitor = new Competitor();
        competitor.setId(30L);
        competitor.setNome(participant.getNome());
        competitor.setEmail(participant.getEmail());
        competitor.setTeam(team);
        competitor.setUserAccount(participant);
        competitor.setAtivo(true);
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.of(competitor));

        RobotDTO robot = new RobotDTO();
        robot.setId(40L);
        robot.setNome("Avulso");
        robot.setTeamId(4L);
        when(robotService.criar(any(RobotDTO.class))).thenReturn(robot);

        RegistrationDTO saved = new RegistrationDTO();
        saved.setId(50L);
        saved.setRobotId(40L);
        saved.setRobotNome("Avulso");
        saved.setStatus(StatusRegistration.APROVADA);
        when(registrationService.criarEntradaManualDev(any(RegistrationDTO.class), eq(dev), eq("Inclusão autorizada")))
                .thenReturn(saved);

        RegistrationDTO result = service.criar(request());

        assertEquals(50L, result.getId());
        assertEquals(StatusRegistration.APROVADA, result.getStatus());

        ArgumentCaptor<RobotDTO> robotCaptor = ArgumentCaptor.forClass(RobotDTO.class);
        verify(robotService).criar(robotCaptor.capture());
        assertEquals(4L, robotCaptor.getValue().getTeamId());

        ArgumentCaptor<RegistrationDTO> registrationCaptor = ArgumentCaptor.forClass(RegistrationDTO.class);
        verify(registrationService).criarEntradaManualDev(
                registrationCaptor.capture(),
                eq(dev),
                eq("Inclusão autorizada"));

        RegistrationDTO registration = registrationCaptor.getValue();
        assertEquals(10L, registration.getCompetitionId());
        assertEquals(20L, registration.getCategoryId());
        assertEquals(4L, registration.getTeamId());
        assertEquals(40L, registration.getRobotId());
        assertEquals(java.util.List.of(30L), registration.getCompetitorIds());
    }

    @Test
    void devNaoPodeCriarEntradaManualParaParticipanteSemEquipeCompetitiva() {
        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(request()));

        assertEquals(
                "O participante ainda não está associado a uma equipe. "
                        + "Peça para ele criar ou ingressar na equipe antes de usar a entrada manual.",
                error.getMessage());

        verifyNoInteractions(robotService, registrationService);
    }

    @Test
    void gestaoNaoPodeUsarEntradaManualDev() {
        UserAccount gestao = new UserAccount();
        gestao.setRole(UserRole.GESTAO);
        gestao.setAtivo(true);
        when(userAccountService.buscarAtual()).thenReturn(gestao);

        assertThrows(AccessDeniedException.class, () -> service.criar(request()));
        verifyNoInteractions(userAccountRepository, competitorRepository, robotService, registrationService);
    }

    private ManualCompetitionEntryRequest request() {
        ManualCompetitionEntryRequest request = new ManualCompetitionEntryRequest();
        request.setCompetitionId(10L);
        request.setCategoryId(20L);
        request.setParticipantUserId(2L);
        request.setRobotNome("Avulso");
        request.setRobotDescricao("Entrada tardia aprovada pela organização.");
        request.setJustificativa("Inclusão autorizada");
        return request;
    }
}
