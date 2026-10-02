package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.RegistrationCompetitorChangeRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationCompositionServiceTest {

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
    void umResponsavelAprovadoJaTornaComposicaoElegivelMesmoComOutroPendente() {
        Contexto c = contexto();
        Competitor maria = competitor(10L, "Maria", c.team());
        Competitor joao = competitor(11L, "João", c.team());

        RobotResponsible mariaLink = responsibility(c.robot(), maria);
        RobotResponsible joaoLink = responsibility(c.robot(), joao);

        when(robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of(mariaLink, joaoLink));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 10L))
                .thenReturn(Optional.of(personal(c.competition(), maria, ParticipantCompetitionRegistrationStatus.APROVADA)));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 11L))
                .thenReturn(Optional.of(personal(c.competition(), joao, ParticipantCompetitionRegistrationStatus.PENDENTE)));
        when(changeRepository.findTopByRegistrationIdAndCompetitorIdAndChangeTypeOrderByIdDesc(
                20L, 10L, RegistrationCompetitorChangeType.ADICIONADO))
                .thenReturn(Optional.empty());
        when(changeRepository.findByRegistrationIdOrderByDataCadastroDesc(20L))
                .thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sincronizarParaAprovacao(c.registration(), null);

        assertEquals(1, c.registration().getCompetitors().size());
        assertTrue(c.registration().getCompetitors().contains(maria));
    }

    @Test
    void todosRejeitadosDevemRejeitarAutomaticamenteInscricaoDoRobo() {
        Contexto c = contexto();
        Competitor a = competitor(10L, "A", c.team());
        Competitor b = competitor(11L, "B", c.team());

        when(registrationRepository.findByRobotIdAndStatusIn(
                5L,
                List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(c.registration()));
        when(robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of(responsibility(c.robot(), a), responsibility(c.robot(), b)));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 10L))
                .thenReturn(Optional.of(personal(c.competition(), a, ParticipantCompetitionRegistrationStatus.REJEITADA)));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 11L))
                .thenReturn(Optional.of(personal(c.competition(), b, ParticipantCompetitionRegistrationStatus.CANCELADA)));
        when(changeRepository.findByRegistrationIdOrderByDataCadastroDesc(20L))
                .thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sincronizarRobot(5L, null, false);

        assertEquals(StatusRegistration.REJEITADA, c.registration().getStatus());
        assertTrue(c.registration().getReviewReason().contains("sem responsável elegível"));
    }

    @Test
    void somentePendenteMantemRoboPendenteSemComposicaoOficial() {
        Contexto c = contexto();
        Competitor joao = competitor(11L, "João", c.team());

        when(registrationRepository.findByRobotIdAndStatusIn(
                5L,
                List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA)))
                .thenReturn(List.of(c.registration()));
        when(robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(5L))
                .thenReturn(List.of(responsibility(c.robot(), joao)));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(1L, 11L))
                .thenReturn(Optional.of(personal(c.competition(), joao, ParticipantCompetitionRegistrationStatus.PENDENTE)));
        when(changeRepository.findByRegistrationIdOrderByDataCadastroDesc(20L))
                .thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sincronizarRobot(5L, null, false);

        assertEquals(StatusRegistration.PENDENTE, c.registration().getStatus());
        assertTrue(c.registration().getCompetitors().isEmpty());
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

        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setDataInicio(LocalDate.now().plusDays(2));
        competition.setDataFim(LocalDate.now().plusDays(3));
        competition.setAtivo(true);

        Registration registration = new Registration();
        registration.setId(20L);
        registration.setCompetition(competition);
        registration.setRobot(robot);
        registration.setTeam(team);
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);

        return new Contexto(team, robot, competition, registration);
    }

    private Competitor competitor(Long id, String nome, Team team) {
        Competitor competitor = new Competitor();
        competitor.setId(id);
        competitor.setNome(nome);
        competitor.setTeam(team);
        competitor.setAtivo(true);
        return competitor;
    }

    private RobotResponsible responsibility(Robot robot, Competitor competitor) {
        RobotResponsible link = new RobotResponsible();
        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setAtivo(true);
        return link;
    }

    private ParticipantCompetitionRegistration personal(
            Competition competition,
            Competitor competitor,
            ParticipantCompetitionRegistrationStatus status) {
        ParticipantCompetitionRegistration personal = new ParticipantCompetitionRegistration();
        personal.setCompetition(competition);
        personal.setCompetitor(competitor);
        personal.setStatus(status);
        personal.setAtivo(true);
        return personal;
    }

    private record Contexto(
            Team team,
            Robot robot,
            Competition competition,
            Registration registration) {}
}
