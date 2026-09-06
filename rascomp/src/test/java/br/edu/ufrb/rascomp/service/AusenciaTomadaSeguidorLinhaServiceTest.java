package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import br.edu.ufrb.rascomp.dto.AusenciaTomadaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.AusenciaTomadaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class AusenciaTomadaSeguidorLinhaServiceTest {

    @Mock private AusenciaTomadaSeguidorLinhaRepository ausenciaRepository;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ConfigFollowRepository configFollowRepository;
    @Mock private UserAccountService userAccountService;

    @InjectMocks private AusenciaTomadaSeguidorLinhaService service;

    private Registration registration;
    private UserAccount organizacao;

    @BeforeEach
    void setup() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setAtivo(true);

        CompetitionCategory category = CompetitionCategory.builder()
                .id(2L)
                .nome("Follow")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        Team team = new Team();
        team.setId(3L);
        team.setNome("Equipe");
        team.setAtivo(true);

        Robot robot = new Robot();
        robot.setId(4L);
        robot.setNome("Chronos");
        robot.setTeam(team);
        robot.setAtivo(true);

        registration = new Registration();
        registration.setId(5L);
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);

        ConfigFollow config = ConfigFollow.builder()
                .competitionCategory(category)
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(120)
                .numeroCheckpoints(5)
                .penalidadePadraoSegundos(10)
                .tempoApresentacaoSegundos(60)
                .build();

        organizacao = new UserAccount();
        organizacao.setId(6L);
        organizacao.setNome("Organização");
        organizacao.setEmail("organizacao@rascomp.local");
        organizacao.setRole(UserRole.ORGANIZACAO);
        organizacao.setAtivo(true);

        when(registrationRepository.findById(5L)).thenReturn(Optional.of(registration));
        when(configFollowRepository.findByCompetitionCategoryId(2L)).thenReturn(Optional.of(config));
    }

    @Test
    void organizacaoPodeMarcarTomadaSemTentativasComoAusente() {
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(ausenciaRepository.save(any(AusenciaTomadaSeguidorLinha.class))).thenAnswer(invocation -> {
            AusenciaTomadaSeguidorLinha entity = invocation.getArgument(0);
            entity.setId(7L);
            return entity;
        });

        AusenciaTomadaSeguidorLinhaDTO result = service.marcar(dto(2));

        assertEquals(2, result.getTomada());
        assertEquals(5L, result.getRegistrationId());
        assertEquals(6L, result.getRegistradoPorId());
    }

    @Test
    void naoPodeMarcarAusenciaDepoisDeTentativaRegistrada() {
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(tentativaRepository.existsByRegistrationIdAndTomada(5L, 1)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.marcar(dto(1)));
        verify(ausenciaRepository, never()).save(any());
    }

    @Test
    void naoPodeDuplicarAusenciaDaMesmaTomada() {
        when(userAccountService.buscarAtual()).thenReturn(organizacao);
        when(ausenciaRepository.existsByRegistrationIdAndTomada(5L, 3)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.marcar(dto(3)));
        verify(ausenciaRepository, never()).save(any());
    }

    @Test
    void participanteNaoPodeMarcarAusencia() {
        UserAccount participante = new UserAccount();
        participante.setId(8L);
        participante.setRole(UserRole.PARTICIPANTE);
        participante.setAtivo(true);
        when(userAccountService.buscarAtual()).thenReturn(participante);

        assertThrows(AccessDeniedException.class, () -> service.marcar(dto(1)));
        verify(ausenciaRepository, never()).save(any());
    }

    private AusenciaTomadaSeguidorLinhaDTO dto(int tomada) {
        AusenciaTomadaSeguidorLinhaDTO dto = new AusenciaTomadaSeguidorLinhaDTO();
        dto.setRegistrationId(5L);
        dto.setTomada(tomada);
        dto.setObservacao("Não compareceu dentro do tempo de apresentação.");
        return dto;
    }
}
