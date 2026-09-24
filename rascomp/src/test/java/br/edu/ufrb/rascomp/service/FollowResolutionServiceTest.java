package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.FollowTakeSchedule;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusChamadaFollow;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class FollowResolutionServiceTest {

    @Mock private ConfigFollowRepository configFollowRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private AusenciaTomadaSeguidorLinhaRepository ausenciaRepository;
    @Mock private FollowTakeScheduleRepository scheduleRepository;
    @Mock private FollowManualResultRepository manualResultRepository;

    @InjectMocks
    private FollowResolutionService service;

    private Registration registration;

    @BeforeEach
    void setup() {
        ConfigFollow config = ConfigFollow.builder()
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(120)
                .numeroCheckpoints(5)
                .penalidadePadraoSegundos(10)
                .tempoApresentacaoSegundos(60)
                .build();

        registration = new Registration();
        registration.setId(10L);

        when(configFollowRepository.findByCompetitionCategoryId(2L))
                .thenReturn(Optional.of(config));
        when(registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        1L,
                        2L,
                        br.edu.ufrb.rascomp.model.Enum.StatusRegistration.APROVADA))
                .thenReturn(List.of(registration));

        for (int tomada = 1; tomada <= 3; tomada++) {
            when(ausenciaRepository.existsByRegistrationIdAndTomada(10L, tomada))
                    .thenReturn(true);
        }

        when(tentativaRepository
                .findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
                        1L, 2L))
                .thenReturn(List.of());

        when(manualResultRepository.existsByCompetitionIdAndCategoryId(1L, 2L))
                .thenReturn(false);
    }

    @Test
    void deveLiberarTomadaExtraAposProgramaNormalSemClassificacao() {
        when(scheduleRepository.existsByCompetitionIdAndCategoryIdAndTomada(1L, 2L, 4))
                .thenReturn(false);

        assertTrue(service.programaNormalConcluido(1L, 2L));
        assertTrue(service.podeCriarTomadaExtra(1L, 2L));
        assertTrue(service.tomadaPermitida(1L, 2L, 3));
        assertFalse(service.tomadaPermitida(1L, 2L, 4));
    }

    @Test
    void decisaoManualDeveEsperarTomadaExtraTerminar() {
        FollowTakeSchedule extra = new FollowTakeSchedule();
        extra.setTomada(4);
        extra.setAtivo(true);
        extra.setStatus(StatusChamadaFollow.AGENDADA);

        when(scheduleRepository.findByCompetitionIdAndCategoryIdAndTomada(1L, 2L, 4))
                .thenReturn(Optional.of(extra));
        when(ausenciaRepository.existsByRegistrationIdAndTomada(10L, 4))
                .thenReturn(false);
        lenient().when(tentativaRepository.countByRegistrationIdAndTomada(10L, 4))
                .thenReturn(0L);

        assertTrue(service.tomadaExtraAtiva(1L, 2L));
        assertTrue(service.tomadaPermitida(1L, 2L, 4));
        assertFalse(service.podeDecidirManualmente(1L, 2L));
    }

    @Test
    void decisaoManualPodeOcorrerDepoisDaExtraSemClassificacao() {
        FollowTakeSchedule extra = new FollowTakeSchedule();
        extra.setTomada(4);
        extra.setAtivo(true);
        extra.setStatus(StatusChamadaFollow.FINALIZADA);

        when(scheduleRepository.findByCompetitionIdAndCategoryIdAndTomada(1L, 2L, 4))
                .thenReturn(Optional.of(extra));
        when(ausenciaRepository.existsByRegistrationIdAndTomada(10L, 4))
                .thenReturn(true);

        assertTrue(service.programaConcluido(1L, 2L));
        assertTrue(service.podeDecidirManualmente(1L, 2L));
    }
}
