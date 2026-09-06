package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.InspecaoSumoRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationIntegrityServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private UserAccountService userAccountService;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private InspecaoSumoRepository inspecaoSumoRepository;
    @Mock private MatchRepository matchRepository;

    @InjectMocks
    private RegistrationService service;

    private Registration registration;
    private Competition competition;

    @BeforeEach
    void setup() {
        Institution institution = new Institution();
        institution.setId(10L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        Team team = new Team();
        team.setId(20L);
        team.setNome("Equipe Teste");
        team.setInstitution(institution);
        team.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(30L);
        robot.setNome("Titan");
        robot.setTeam(team);
        robot.setAtivo(true);

        CompetitionCategory category = CompetitionCategory.builder()
                .id(40L)
                .nome("Mini Sumô")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        competition = new Competition();
        competition.setId(50L);
        competition.setNome("RRC Teste");
        competition.setInicioInscricoes(LocalDate.now().minusDays(5));
        competition.setFimInscricoes(LocalDate.now().plusDays(5));
        competition.setDataInicio(LocalDate.now().plusDays(10));
        competition.setDataFim(LocalDate.now().plusDays(11));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setAtivo(true);

        registration = new Registration();
        registration.setId(1L);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(new LinkedHashSet<>());
        registration.setAtivo(true);
        registration.setStatus(StatusRegistration.PENDENTE);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reativacaoDeveRespeitarJanelaDeInscricoes() {
        registration.setStatus(StatusRegistration.CANCELADA);
        registration.setAtivo(false);
        competition.setFimInscricoes(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> service.reativar(1L));

        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void organizacaoPodeReabrirRejeitadaDentroDaJanela() {
        registration.setStatus(StatusRegistration.REJEITADA);
        registration.setAtivo(false);

        RegistrationDTO result = service.reativar(1L);

        assertEquals(StatusRegistration.PENDENTE, result.getStatus());
        assertEquals(true, result.getAtivo());
    }

    @Test
    void participanteNaoPodeCancelarDiretamenteInscricaoAprovada() {
        registration.setStatus(StatusRegistration.APROVADA);

        assertThrows(IllegalArgumentException.class, () -> service.cancelarPorParticipante(1L));

        assertEquals(StatusRegistration.APROVADA, registration.getStatus());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void organizacaoCancelaAprovadaSemHistoricoComoCancelada() {
        registration.setStatus(StatusRegistration.APROVADA);

        service.deletar(1L);

        assertEquals(StatusRegistration.CANCELADA, registration.getStatus());
        assertEquals(false, registration.getAtivo());
    }

    @Test
    void organizacaoMarcaComoDesistenteQuandoJaExisteAtividadeCompetitiva() {
        registration.setStatus(StatusRegistration.APROVADA);
        when(tentativaRepository.existsByRegistrationId(1L)).thenReturn(true);

        service.deletar(1L);

        assertEquals(StatusRegistration.DESISTENTE, registration.getStatus());
        assertEquals(false, registration.getAtivo());
    }

    @Test
    void inscricaoAprovadaNaoPodeSerEditadaPeloPutComum() {
        registration.setStatus(StatusRegistration.APROVADA);
        RegistrationDTO dto = new RegistrationDTO();

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(1L, dto));
        verify(registrationRepository, never()).save(any());
    }
}
