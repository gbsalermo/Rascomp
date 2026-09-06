package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RegistrationCancellationRequest;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCancellationRequest;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.RegistrationCancellationRequestRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationCancellationRequestServiceTest {

    @Mock private RegistrationCancellationRequestRepository requestRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private RegistrationService registrationService;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private RegistrationCancellationRequestService service;

    private Registration registration;
    private UserAccount participante;
    private UserAccount organizacao;

    @BeforeEach
    void setup() {
        Competition competition = new Competition();
        competition.setId(10L);
        competition.setNome("RRC Teste");
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        competition.setAtivo(true);
        competition.setInicioInscricoes(LocalDate.now().minusDays(10));
        competition.setFimInscricoes(LocalDate.now().minusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(2));
        competition.setDataFim(LocalDate.now().plusDays(3));

        Team team = new Team();
        team.setId(11L);
        team.setNome("Equipe Teste");

        Robot robot = new Robot();
        robot.setId(12L);
        robot.setNome("Titan");
        robot.setTeam(team);

        registration = new Registration();
        registration.setId(20L);
        registration.setCompetition(competition);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);

        participante = new UserAccount();
        participante.setId(30L);
        participante.setNome("Participante");
        participante.setRole(UserRole.PARTICIPANTE);
        participante.setAtivo(true);

        organizacao = new UserAccount();
        organizacao.setId(40L);
        organizacao.setNome("Organização");
        organizacao.setRole(UserRole.ORGANIZACAO);
        organizacao.setAtivo(true);
    }

    @Test
    void participantePodeSolicitarCancelamentoDeAprovada() {
        when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
        when(requestRepository.existsByRegistrationIdAndStatus(20L, StatusCancellationRequest.PENDENTE)).thenReturn(false);
        when(requestRepository.save(any(RegistrationCancellationRequest.class))).thenAnswer(invocation -> {
            RegistrationCancellationRequest entity = invocation.getArgument(0);
            entity.setId(50L);
            return entity;
        });

        var result = service.solicitar(20L, participante, "Não poderemos comparecer");

        assertEquals(50L, result.getId());
        assertEquals(StatusCancellationRequest.PENDENTE, result.getStatus());
        assertEquals("Não poderemos comparecer", result.getMotivo());
    }

    @Test
    void naoPermiteDuasSolicitacoesPendentes() {
        when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
        when(requestRepository.existsByRegistrationIdAndStatus(20L, StatusCancellationRequest.PENDENTE)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(20L, participante, "Motivo"));

        verify(requestRepository, never()).save(any());
    }

    @Test
    void organizacaoAprovaEServiceConcluiCancelamento() {
        RegistrationCancellationRequest request = pendingRequest();
        when(requestRepository.findById(60L)).thenReturn(Optional.of(request));
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(requestRepository.save(any(RegistrationCancellationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.aprovar(60L, "Deferido");

        verify(registrationService).cancelarAprovadaPorSolicitacao(20L);
        assertEquals(StatusCancellationRequest.APROVADA, result.getStatus());
        assertEquals("Deferido", result.getResposta());
    }

    @Test
    void rejeicaoNaoCancelaInscricao() {
        RegistrationCancellationRequest request = pendingRequest();
        when(requestRepository.findById(60L)).thenReturn(Optional.of(request));
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(requestRepository.save(any(RegistrationCancellationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.rejeitar(60L, "Inscrição já comprometida com a programação");

        verify(registrationService, never()).cancelarAprovadaPorSolicitacao(any());
        assertEquals(StatusCancellationRequest.REJEITADA, result.getStatus());
    }

    private RegistrationCancellationRequest pendingRequest() {
        RegistrationCancellationRequest request = new RegistrationCancellationRequest();
        request.setId(60L);
        request.setRegistration(registration);
        request.setRequestedByUser(participante);
        request.setStatus(StatusCancellationRequest.PENDENTE);
        request.setMotivo("Não poderemos comparecer");
        return request;
    }
}
