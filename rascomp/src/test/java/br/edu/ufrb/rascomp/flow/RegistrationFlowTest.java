package br.edu.ufrb.rascomp.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationStatusChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import br.edu.ufrb.rascomp.service.RegistrationService;
import br.edu.ufrb.rascomp.service.RegistrationStatusHistoryService;
import br.edu.ufrb.rascomp.service.TentativaSeguidorLinhaService;

@SpringBootTest
@ActiveProfiles("flowtest")
class RegistrationFlowTest extends IntegrationFlowTestSupport {

    @Autowired private RegistrationService registrationService;
    @Autowired private RegistrationStatusHistoryService statusHistoryService;
    @Autowired private TentativaSeguidorLinhaService tentativaService;
    @Autowired private CompetitorRepository competitorRepository;
    @Autowired private RobotResponsibleRepository robotResponsibleRepository;
    @Autowired private ParticipantCompetitionRegistrationRepository participantRegistrationRepository;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void deveCriarAprovarECancelarSemHistoricoComoCancelada() {
        var organizacao = organizacaoAutenticada();
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        CompetitionCategory category = followCategory();
        Team team = team();
        Robot robot = robot(team);
        Competitor competitor = competitor(team);
        responsibility(robot, competitor, organizacao);

        RegistrationDTO criada = registrationService.criar(
                request(competition, category, team, robot, competitor));
        assertEquals(StatusRegistration.PENDENTE, criada.getStatus());
        assertTrue(criada.getAtivo());

        Registration persistida = registrationRepository.findById(criada.getId()).orElseThrow();
        persistida.setPaymentReceiptStorageKey("flow/receipt.pdf");
        persistida.setPaymentReceiptOriginalName("receipt.pdf");
        persistida.setPaymentReceiptContentType("application/pdf");
        registrationRepository.save(persistida);

        ParticipantCompetitionRegistration pessoal = new ParticipantCompetitionRegistration();
        pessoal.setCompetition(competition);
        pessoal.setCompetitor(competitor);
        pessoal.setRequestedByUser(organizacao);
        pessoal.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);
        pessoal.setAtivo(true);
        participantRegistrationRepository.save(pessoal);

        RegistrationDTO revisao = registrationService.buscarPorId(criada.getId());
        revisao.setStatus(StatusRegistration.APROVADA);
        RegistrationDTO aprovada = registrationService.atualizar(criada.getId(), revisao);

        assertEquals(StatusRegistration.APROVADA, aprovada.getStatus());
        assertEquals(organizacao.getId(), aprovada.getReviewedByUserId());
        assertNotNull(aprovada.getReviewedAt());

        RegistrationDTO cancelada = registrationService.cancelarAprovadaPorSolicitacao(criada.getId());

        assertEquals(StatusRegistration.CANCELADA, cancelada.getStatus());
        assertFalse(cancelada.getAtivo());

        var historico = statusHistoryService.listar(criada.getId());
        assertEquals(3, historico.size());
        assertEquals(RegistrationStatusChangeType.CANCELAMENTO, historico.get(0).getChangeType());
        assertEquals(StatusRegistration.CANCELADA, historico.get(0).getNewStatus());
        assertEquals(RegistrationStatusChangeType.APROVACAO, historico.get(1).getChangeType());
        assertEquals(StatusRegistration.APROVADA, historico.get(1).getNewStatus());
        assertEquals(RegistrationStatusChangeType.CRIACAO, historico.get(2).getChangeType());
        assertEquals(StatusRegistration.PENDENTE, historico.get(2).getNewStatus());
    }

    @Test
    void cancelamentoDepoisDeAtividadeFollowDeveVirarDesistente() {
        organizacaoAutenticada();
        Competition competition = competition(StatusCompetition.EM_ANDAMENTO);
        CompetitionCategory category = followCategory();
        Team team = team();
        Registration registration = approvedRegistration(competition, category, team, robot(team));

        TentativaSeguidorLinhaDTO tentativa = tentativa(registration, 1, 1, "41.250", true, true);
        tentativaService.criar(tentativa);

        RegistrationDTO cancelada = registrationService.cancelarAprovadaPorSolicitacao(registration.getId());

        assertEquals(StatusRegistration.DESISTENTE, cancelada.getStatus());
        assertFalse(cancelada.getAtivo());
    }

    @Test
    void participanteNaoPodeCancelarDiretamenteInscricaoAprovada() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = followCategory();
        Team team = team();
        Registration registration = approvedRegistration(competition, category, team, robot(team));

        assertThrows(IllegalArgumentException.class,
                () -> registrationService.cancelarPorParticipante(registration.getId()));

        Registration persistida = registrationRepository.findById(registration.getId()).orElseThrow();
        assertEquals(StatusRegistration.APROVADA, persistida.getStatus());
        assertTrue(persistida.getAtivo());
    }

    private RegistrationDTO request(
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot,
            Competitor competitor) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(competition.getId());
        dto.setCategoryId(category.getId());
        dto.setTeamId(team.getId());
        dto.setRobotId(robot.getId());
        dto.setCompetitorIds(List.of(competitor.getId()));
        dto.setObservacao("Inscricao criada no fluxo integrado.");
        return dto;
    }

    private Competitor competitor(Team team) {
        Competitor competitor = new Competitor();
        competitor.setNome(unique("Competidor"));
        competitor.setEmail(unique("competidor").toLowerCase() + "@flow.local");
        competitor.setTeam(team);
        competitor.setAtivo(true);
        return competitorRepository.save(competitor);
    }

    private void responsibility(Robot robot, Competitor competitor, br.edu.ufrb.rascomp.model.UserAccount actor) {
        RobotResponsible link = new RobotResponsible();
        link.setRobot(robot);
        link.setCompetitor(competitor);
        link.setCreatedByUser(actor);
        link.setAtivo(true);
        robotResponsibleRepository.save(link);
    }

    private TentativaSeguidorLinhaDTO tentativa(
            Registration registration,
            int tomada,
            int numero,
            String tempo,
            boolean concluida,
            boolean valida) {
        TentativaSeguidorLinhaDTO dto = new TentativaSeguidorLinhaDTO();
        dto.setRegistrationId(registration.getId());
        dto.setTomada(tomada);
        dto.setNumeroTentativa(numero);
        dto.setTempoSegundos(tempo == null ? null : new BigDecimal(tempo));
        dto.setCheckpointsAlcancados(concluida ? 5 : 0);
        dto.setPenalidadeSegundos(0);
        dto.setConcluida(concluida);
        dto.setValida(valida);
        return dto;
    }
}
