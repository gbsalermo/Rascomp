package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;

@ExtendWith(MockitoExtension.class)
class TentativaSeguidorLinhaServiceTest {

    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ConfigFollowRepository configFollowRepository;

    @InjectMocks
    private TentativaSeguidorLinhaService service;

    private Registration registration;

    @BeforeEach
    void setUp() {
        CompetitionCategory category = CompetitionCategory.builder()
                .id(3L)
                .nome("Seguidor de Linha")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        registration = new Registration();
        registration.setId(1L);
        registration.setCategory(category);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);

        ConfigFollow config = ConfigFollow.builder()
                .competitionCategory(category)
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(180)
                .numeroCheckpoints(5)
                .build();

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(configFollowRepository.findByCompetitionCategoryId(3L)).thenReturn(Optional.of(config));
    }

    @Test
    void deveRejeitarTomadaAcimaDoLimiteConfigurado() {
        TentativaSeguidorLinhaDTO dto = tentativaBase();
        dto.setTomada(4);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
    }

    @Test
    void tempoAcimaDoMaximoDevePersistirComoTentativaInvalida() {
        TentativaSeguidorLinhaDTO dto = tentativaBase();
        dto.setTempoSegundos(new BigDecimal("181.500"));

        when(tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(1L, 2, 1))
                .thenReturn(false);
        when(tentativaRepository.save(any(TentativaSeguidorLinha.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TentativaSeguidorLinhaDTO resultado = service.criar(dto);

        assertFalse(resultado.getValida());
    }

    private TentativaSeguidorLinhaDTO tentativaBase() {
        TentativaSeguidorLinhaDTO dto = new TentativaSeguidorLinhaDTO();
        dto.setRegistrationId(1L);
        dto.setTomada(2);
        dto.setNumeroTentativa(1);
        dto.setTempoSegundos(new BigDecimal("45.350"));
        dto.setCheckpointsAlcancados(5);
        dto.setPenalidadeSegundos(0);
        dto.setConcluida(true);
        dto.setValida(true);
        dto.setObservacao("Teste automatizado");
        return dto;
    }
}
