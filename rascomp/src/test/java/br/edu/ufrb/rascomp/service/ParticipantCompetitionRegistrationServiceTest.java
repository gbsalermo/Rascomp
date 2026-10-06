package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationReviewRequest;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.ParticipantRegistrationStatusHistory;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.ParticipantRegistrationStatusHistoryRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;

@ExtendWith(MockitoExtension.class)
class ParticipantCompetitionRegistrationServiceTest {

    @Mock private ParticipantCompetitionRegistrationRepository repository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private RobotResponsibleRepository robotResponsibleRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private UserAccountService userAccountService;
    @Mock private CompetitionContextService competitionContextService;
    @Mock private RegistrationReceiptStorageService receiptStorageService;
    @Mock private ParticipantRegistrationStatusHistoryRepository statusHistoryRepository;
    @Mock private RegistrationCompositionService compositionService;
    @Mock private RegistrationLotService registrationLotService;

    @InjectMocks
    private ParticipantCompetitionRegistrationService service;

    @Test
    void participanteDeveEnviarInscricaoIndividualPendenteComComprovante() {
        Contexto c = contexto();

        ParticipantCompetitionRegistrationRequest request =
                new ParticipantCompetitionRegistrationRequest();
        request.setCompetitionId(c.competition().getId());
        request.setObservacao("Inscrição individual para QA.");

        MockMultipartFile comprovante = comprovante();

        when(userAccountService.buscarAtual()).thenReturn(c.user());
        when(competitorRepository.findByUserAccountId(c.user().getId()))
                .thenReturn(Optional.of(c.competitor()));
        when(competitionRepository.findById(c.competition().getId()))
                .thenReturn(Optional.of(c.competition()));
        when(repository.existsByCompetitionIdAndCompetitorId(
                c.competition().getId(), c.competitor().getId()))
                .thenReturn(false);
        when(receiptStorageService.armazenar(
                "participante",
                c.competition().getId(),
                c.competitor().getId(),
                comprovante))
                .thenReturn(new RegistrationReceiptStorageService.StoredReceipt(
                        "participante/1/5/receipt.pdf",
                        "pagamento.pdf",
                        "application/pdf"));
        when(repository.save(any(ParticipantCompetitionRegistration.class)))
                .thenAnswer(invocation -> {
                    ParticipantCompetitionRegistration entity = invocation.getArgument(0);
                    entity.setId(50L);
                    return entity;
                });
        when(statusHistoryRepository.save(any(ParticipantRegistrationStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(robotResponsibleRepository.findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(
                c.competitor().getId()))
                .thenReturn(List.of());

        var resultado = service.criarParaParticipanteAtual(request, comprovante);

        assertEquals(50L, resultado.getId());
        assertEquals(ParticipantCompetitionRegistrationStatus.PENDENTE, resultado.getStatus());
        assertEquals(true, resultado.getComprovanteDisponivel());
        assertEquals("pagamento.pdf", resultado.getComprovanteNome());
    }

    @Test
    void gestaoDeveAprovarInscricaoIndividualPendenteESincronizarRobos() {
        Contexto c = contexto();
        UserAccount gestao = gestao();

        ParticipantCompetitionRegistration entity = inscricao(c);
        ParticipantCompetitionRegistrationReviewRequest request =
                new ParticipantCompetitionRegistrationReviewRequest();
        request.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);

        when(repository.findById(50L)).thenReturn(Optional.of(entity));
        when(userAccountService.buscarAtual()).thenReturn(gestao);
        when(repository.save(entity)).thenReturn(entity);
        when(statusHistoryRepository.save(any(ParticipantRegistrationStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(robotResponsibleRepository.findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(
                c.competitor().getId()))
                .thenReturn(List.of());

        var resultado = service.revisar(50L, request);

        assertEquals(ParticipantCompetitionRegistrationStatus.APROVADA, resultado.getStatus());
        assertEquals(99L, resultado.getReviewedByUserId());
        verify(compositionService).sincronizarPorInscricaoPessoal(entity, gestao);
    }

    @Test
    void gestaoNaoPodeRejeitarDefinitivamenteLiderAtualDaEquipe() {
        Contexto c = contexto();
        c.team().setResponsibleUser(c.user());
        UserAccount gestao = gestao();

        ParticipantCompetitionRegistration entity = inscricao(c);
        ParticipantCompetitionRegistrationReviewRequest request =
                new ParticipantCompetitionRegistrationReviewRequest();
        request.setStatus(ParticipantCompetitionRegistrationStatus.REJEITADA);
        request.setMotivo("Pagamento inválido.");

        when(repository.findById(50L)).thenReturn(Optional.of(entity));
        when(userAccountService.buscarAtual()).thenReturn(gestao);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.revisar(50L, request));

        assertEquals(true, ex.getMessage().contains("líder"));
    }

    @Test
    void gestaoPodeSolicitarCorrecaoAoLiderSemRejeitarEquipe() {
        Contexto c = contexto();
        c.team().setResponsibleUser(c.user());
        UserAccount gestao = gestao();

        ParticipantCompetitionRegistration entity = inscricao(c);
        ParticipantCompetitionRegistrationReviewRequest request =
                new ParticipantCompetitionRegistrationReviewRequest();
        request.setStatus(ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA);
        request.setMotivo("Reenvie um comprovante legível.");

        when(repository.findById(50L)).thenReturn(Optional.of(entity));
        when(userAccountService.buscarAtual()).thenReturn(gestao);
        when(repository.save(entity)).thenReturn(entity);
        when(statusHistoryRepository.save(any(ParticipantRegistrationStatusHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(robotResponsibleRepository.findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(
                c.competitor().getId()))
                .thenReturn(List.of());

        var resultado = service.revisar(50L, request);

        assertEquals(
                ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA,
                resultado.getStatus());
        verify(compositionService).sincronizarPorInscricaoPessoal(entity, gestao);
    }

    private ParticipantCompetitionRegistration inscricao(Contexto c) {
        ParticipantCompetitionRegistration entity = new ParticipantCompetitionRegistration();
        entity.setId(50L);
        entity.setCompetition(c.competition());
        entity.setCompetitor(c.competitor());
        entity.setRequestedByUser(c.user());
        entity.setStatus(ParticipantCompetitionRegistrationStatus.PENDENTE);
        entity.setAtivo(true);
        return entity;
    }

    private MockMultipartFile comprovante() {
        return new MockMultipartFile(
                "comprovante",
                "pagamento.pdf",
                "application/pdf",
                "%PDF-1.4 teste".getBytes());
    }

    private UserAccount gestao() {
        UserAccount gestao = new UserAccount();
        gestao.setId(99L);
        gestao.setNome("Gestão");
        gestao.setRole(UserRole.GESTAO);
        gestao.setAtivo(true);
        return gestao;
    }

    private Contexto contexto() {
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setNome("Gabriel");
        user.setEmail("gabriel@teste.local");
        user.setRole(UserRole.PARTICIPANTE);
        user.setAtivo(true);

        Team team = new Team();
        team.setId(3L);
        team.setNome("Equipe");
        team.setInstitution(institution);
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(5L);
        competitor.setNome("Gabriel");
        competitor.setEmail(user.getEmail());
        competitor.setUserAccount(user);
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setInicioInscricoes(LocalDate.now().minusDays(1));
        competition.setFimInscricoes(LocalDate.now().plusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(5));
        competition.setDataFim(LocalDate.now().plusDays(6));

        return new Contexto(user, team, competitor, competition);
    }

    private record Contexto(
            UserAccount user,
            Team team,
            Competitor competitor,
            Competition competition) {}
}
