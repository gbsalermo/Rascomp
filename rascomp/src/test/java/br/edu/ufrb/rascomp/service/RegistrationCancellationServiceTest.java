package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationCancellationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private UserAccountService userAccountService;
    @Mock private MatchRepository matchRepository;
    @Mock private TentativaSeguidorLinhaRepository tentativaSeguidorLinhaRepository;

    @InjectMocks private RegistrationService service;

    private Registration registration;
    private Competition competition;

    @BeforeEach
    void setup() {
        competition = new Competition();
        competition.setId(10L);
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);

        registration = new Registration();
        registration.setId(20L);
        registration.setCompetition(competition);
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);

        when(registrationRepository.findById(20L)).thenReturn(Optional.of(registration));
    }

    @Test
    void deveCancelarInscricaoPendenteSemHistoricoCompetitivo() {
        service.deletar(20L);

        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        assertEquals(false, registration.getAtivo());
        verify(registrationRepository).save(registration);
    }

    @Test
    void deveCancelarInscricaoAprovadaAposEncerramentoDasInscricoesSemHistoricoCompetitivo() {
        registration.setStatus(StatusRegistration.APROVADA);
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);

        service.deletar(20L);

        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        assertEquals(false, registration.getAtivo());
        verify(registrationRepository).save(registration);
    }

    @ParameterizedTest
    @EnumSource(value = StatusRegistration.class, names = {"REJEITADA", "CANCELADA", "DESCLASSIFICADA"})
    void deveBloquearCancelamentoDeStatusQueNaoSejaPendenteOuAprovada(StatusRegistration status) {
        registration.setStatus(status);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletar(20L));

        assertEquals("Somente inscrições pendentes ou aprovadas podem ser canceladas.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(status, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @ParameterizedTest
    @EnumSource(value = StatusCompetition.class, names = {"EM_ANDAMENTO", "FINALIZADA", "CANCELADA"})
    void deveBloquearCancelamentoEmEstadoCompetitivoProtegido(StatusCompetition statusCompetition) {
        registration.setStatus(StatusRegistration.APROVADA);
        competition.setStatus(statusCompetition);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletar(20L));

        assertEquals("A inscrição não pode ser cancelada neste estado da competição.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.APROVADA, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void deveBloquearCancelamentoQuandoInscricaoJaParticipouDeChave() {
        registration.setStatus(StatusRegistration.APROVADA);
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        when(matchRepository.existsByRegistrationAIdOrRegistrationBId(20L, 20L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletar(20L));

        assertEquals("A inscrição não pode ser cancelada pois já possui histórico competitivo.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.APROVADA, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void deveBloquearCancelamentoQuandoInscricaoJaPossuiTentativaFollow() {
        registration.setStatus(StatusRegistration.APROVADA);
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        when(tentativaSeguidorLinhaRepository.existsByRegistrationId(20L)).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.deletar(20L));

        assertEquals("A inscrição não pode ser cancelada pois já possui histórico competitivo.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.APROVADA, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void deveBloquearCancelamentoDiretoPorStatusNoPut() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setStatus(StatusRegistration.CANCELADA);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(20L, dto));

        assertEquals("Use o fluxo de cancelamento para cancelar ou inativar uma inscrição.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.PENDENTE, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
        verify(competitionRepository, never()).findById(any());
    }

    @Test
    void deveBloquearInativacaoDiretaNoPut() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setAtivo(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(20L, dto));

        assertEquals("Use o fluxo de cancelamento para cancelar ou inativar uma inscrição.", error.getMessage());
        assertTrue(registration.getAtivo());
        assertEquals(StatusRegistration.PENDENTE, registration.getStatus());
        verify(registrationRepository, never()).save(any(Registration.class));
        verify(competitionRepository, never()).findById(any());
    }
}
