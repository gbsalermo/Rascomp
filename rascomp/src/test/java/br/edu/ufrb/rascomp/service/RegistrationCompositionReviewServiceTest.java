package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationCompetitorChangeReviewRequest;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RegistrationCompetitorChange;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.RegistrationCompetitorChangeRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationCompositionReviewServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private RobotResponsibleRepository robotResponsibleRepository;
    @Mock private ParticipantCompetitionRegistrationRepository participantRegistrationRepository;
    @Mock private RegistrationCompetitorChangeRepository changeRepository;
    @Mock private RegistrationStatusHistoryService statusHistoryService;
    @Mock private UserAccountService userAccountService;
    @Mock private CompetitionContextService competitionContextService;

    @InjectMocks
    private RegistrationCompositionService service;

    @Test
    void reinclusaoDeveGerarNovaNotificacaoMesmoAposEventoAnterior() {
        Contexto c = contexto();
        RobotResponsible link = responsibility(c.robot(), c.competitor(), true);

        when(registrationRepository.findByRobotIdAndStatusIn(
                5L,
                List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(c.registration()));
        when(robotResponsibleRepository.findByRobotIdAndCompetitorId(5L, 10L))
                .thenReturn(Optional.of(link));
        when(changeRepository.save(any(RegistrationCompetitorChange.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sinalizarMudancaResponsabilidade(
                5L,
                10L,
                RegistrationCompetitorChangeType.ADICIONADO,
                c.actor());

        ArgumentCaptor<RegistrationCompetitorChange> captor =
                ArgumentCaptor.forClass(RegistrationCompetitorChange.class);
        verify(changeRepository).save(captor.capture());

        assertEquals(
                RegistrationCompetitorChangeType.ADICIONADO,
                captor.getValue().getChangeType());
        assertEquals(
                RegistrationCompetitorChangeStatus.PENDENTE_REVISAO,
                captor.getValue().getStatus());
    }

    @Test
    void vetoDeRemocaoDeveRestaurarResponsabilidadeDoLider() {
        Contexto c = contexto();
        c.registration().setStatus(StatusRegistration.APROVADA);

        RobotResponsible link = responsibility(c.robot(), c.competitor(), false);

        RegistrationCompetitorChange change = new RegistrationCompetitorChange();
        change.setId(50L);
        change.setRegistration(c.registration());
        change.setCompetitor(c.competitor());
        change.setChangeType(RegistrationCompetitorChangeType.REMOVIDO);
        change.setStatus(RegistrationCompetitorChangeStatus.PENDENTE_REVISAO);
        change.setActorUser(c.actor());

        ParticipantCompetitionRegistration personal = new ParticipantCompetitionRegistration();
        personal.setCompetition(c.competition());
        personal.setCompetitor(c.competitor());
        personal.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);
        personal.setAtivo(true);

        UserAccount gestao = new UserAccount();
        gestao.setId(99L);
        gestao.setNome("Gestão");
        gestao.setRole(UserRole.GESTAO);
        gestao.setAtivo(true);

        RegistrationCompetitorChangeReviewRequest request =
                new RegistrationCompetitorChangeReviewRequest();
        request.setStatus(RegistrationCompetitorChangeStatus.VETADA);
        request.setMotivo("Responsável deve permanecer nesta edição.");

        when(userAccountService.buscarAtual()).thenReturn(gestao);
        when(changeRepository.findById(50L)).thenReturn(Optional.of(change));
        when(changeRepository.save(change)).thenReturn(change);
        when(robotResponsibleRepository.findByRobotIdAndCompetitorId(5L, 10L))
                .thenReturn(Optional.of(link));
        when(robotResponsibleRepository.save(link)).thenReturn(link);
        when(robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of(link));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 10L))
                .thenReturn(Optional.of(personal));
        when(changeRepository.findTopByRegistrationIdAndCompetitorIdAndChangeTypeOrderByIdDesc(
                20L, 10L, RegistrationCompetitorChangeType.ADICIONADO))
                .thenReturn(Optional.empty());
        when(changeRepository.findByRegistrationIdOrderByDataCadastroDesc(20L))
                .thenReturn(List.of(change));
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.revisarMudanca(50L, request);

        assertEquals(true, link.getAtivo());
        assertTrue(c.registration().getCompetitors().contains(c.competitor()));
        assertEquals(
                RegistrationCompetitorChangeStatus.VETADA,
                change.getStatus());
    }

    @Test
    void mudancasSemVetoDevemSerConsolidadasNoInicioDaCompeticao() {
        Contexto c = contexto();
        RegistrationCompetitorChange change = new RegistrationCompetitorChange();
        change.setId(60L);
        change.setRegistration(c.registration());
        change.setCompetitor(c.competitor());
        change.setChangeType(RegistrationCompetitorChangeType.ADICIONADO);
        change.setStatus(RegistrationCompetitorChangeStatus.PENDENTE_REVISAO);

        when(changeRepository.findByRegistrationCompetitionIdAndStatusOrderByDataCadastroDesc(
                1L,
                RegistrationCompetitorChangeStatus.PENDENTE_REVISAO))
                .thenReturn(List.of(change));
        when(changeRepository.save(change)).thenReturn(change);

        service.consolidarPendentesDaCompeticao(1L, c.actor());

        assertEquals(
                RegistrationCompetitorChangeStatus.MANTIDA,
                change.getStatus());
        assertEquals(c.actor().getId(), change.getReviewedByUser().getId());
        assertTrue(change.getReason().contains("ausência de veto"));
    }

    private Contexto contexto() {
        Team team = new Team();
        team.setId(3L);
        team.setNome("Equipe");

        Robot robot = new Robot();
        robot.setId(5L);
        robot.setNome("Vespa");
        robot.setTeam(team);
        robot.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(10L);
        competitor.setNome("Membro");
        competitor.setTeam(team);
        competitor.setAtivo(true);

        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setDataInicio(LocalDate.now().plusDays(5));
        competition.setDataFim(LocalDate.now().plusDays(6));
        competition.setAtivo(true);

        Registration registration = new Registration();
        registration.setId(20L);
        registration.setCompetition(competition);
        registration.setRobot(robot);
        registration.setTeam(team);
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);

        UserAccount actor = new UserAccount();
        actor.setId(7L);
        actor.setNome("Líder");
        actor.setRole(UserRole.PARTICIPANTE);
        actor.setAtivo(true);

        return new Contexto(
                team,
                robot,
                competitor,
                competition,
                registration,
                actor);
    }

    private RobotResponsible responsibility(
            Robot robot,
            Competitor competitor,
            boolean ativo) {
        RobotResponsible link = new RobotResponsible();
        link.setId(30L);
        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setAtivo(ativo);
        return link;
    }

    private record Contexto(
            Team team,
            Robot robot,
            Competitor competitor,
            Competition competition,
            Registration registration,
            UserAccount actor) {}
}
