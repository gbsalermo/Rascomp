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
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.service.RegistrationService;
import br.edu.ufrb.rascomp.service.TentativaSeguidorLinhaService;

@SpringBootTest
@ActiveProfiles("flowtest")
class RegistrationFlowTest extends IntegrationFlowTestSupport {

    @Autowired private RegistrationService registrationService;
    @Autowired private TentativaSeguidorLinhaService tentativaService;

    @AfterEach
    void clearSecurity() {
        limparAutenticacao();
    }

    @Test
    void deveCriarAprovarECancelarSemHistoricoComoCancelada() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        CompetitionCategory category = followCategory();
        Team team = team();
        Robot robot = robot(team);

        RegistrationDTO criada = registrationService.criar(request(competition, category, team, robot));
        assertEquals(StatusRegistration.PENDENTE, criada.getStatus());
        assertTrue(criada.getAtivo());

        var organizacao = organizacaoAutenticada();
        RegistrationDTO revisao = registrationService.buscarPorId(criada.getId());
        revisao.setStatus(StatusRegistration.APROVADA);
        RegistrationDTO aprovada = registrationService.atualizar(criada.getId(), revisao);

        assertEquals(StatusRegistration.APROVADA, aprovada.getStatus());
        assertEquals(organizacao.getId(), aprovada.getReviewedByUserId());
        assertNotNull(aprovada.getReviewedAt());

        RegistrationDTO cancelada = registrationService.cancelarAprovadaPorSolicitacao(criada.getId());

        assertEquals(StatusRegistration.CANCELADA, cancelada.getStatus());
        assertFalse(cancelada.getAtivo());
    }

    @Test
    void cancelamentoDepoisDeAtividadeFollowDeveVirarDesistente() {
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
            Robot robot) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setCompetitionId(competition.getId());
        dto.setCategoryId(category.getId());
        dto.setTeamId(team.getId());
        dto.setRobotId(robot.getId());
        dto.setCompetitorIds(List.of());
        dto.setObservacao("Inscricao criada no fluxo integrado.");
        return dto;
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
