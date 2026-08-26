package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class TentativaSeguidorLinhaRulesTest {

    @Mock private TentativaSeguidorLinhaRepository tentativaRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ConfigFollowRepository configFollowRepository;

    @InjectMocks private TentativaSeguidorLinhaService service;

    @BeforeEach
    void setup() {
        CompetitionCategory category = CompetitionCategory.builder()
                .id(7L).nome("Follow").modalidade(Modalidade.FOLLOW_LINE).ativo(true).build();
        Registration registration = new Registration();
        registration.setId(8L);
        registration.setCategory(category);
        registration.setStatus(StatusRegistration.APROVADA);
        registration.setAtivo(true);

        ConfigFollow config = ConfigFollow.builder()
                .competitionCategory(category)
                .numeroTomadas(3)
                .tentativasPorTomada(3)
                .maxTempoSegundos(120)
                .numeroCheckpoints(5)
                .build();

        when(registrationRepository.findById(8L)).thenReturn(Optional.of(registration));
        when(configFollowRepository.findByCompetitionCategoryId(7L)).thenReturn(Optional.of(config));
    }

    @Test
    void deveBloquearTentativaDuplicadaDentroDaMesmaTomada() {
        when(tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(8L, 2, 2)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.criar(base(2, 2)));
    }

    @Test
    void deveBloquearNumeroDeTentativaAcimaDoConfigurado() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(base(1, 4)));
    }

    @Test
    void deveBloquearCheckpointAcimaDoConfigurado() {
        TentativaSeguidorLinhaDTO dto = base(1, 1);
        dto.setCheckpointsAlcancados(6);
        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
    }

    @Test
    void tempoAcimaDoLimiteDeveSerPersistidoMasInvalido() {
        TentativaSeguidorLinhaDTO dto = base(3, 1);
        dto.setTempoSegundos(new BigDecimal("120.001"));
        when(tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(8L, 3, 1)).thenReturn(false);
        when(tentativaRepository.save(any(TentativaSeguidorLinha.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TentativaSeguidorLinhaDTO result = service.criar(dto);
        assertFalse(result.getValida());
        assertEquals(new BigDecimal("120.001"), result.getTempoSegundos());
    }

    @Test
    void penalidadeDeveComporTempoFinalDoDto() {
        TentativaSeguidorLinhaDTO dto = base(1, 2);
        dto.setTempoSegundos(new BigDecimal("40.250"));
        dto.setPenalidadeSegundos(2);
        when(tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(8L, 1, 2)).thenReturn(false);
        when(tentativaRepository.save(any(TentativaSeguidorLinha.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TentativaSeguidorLinhaDTO result = service.criar(dto);
        assertEquals(0, new BigDecimal("42.250").compareTo(result.getTempoFinalSegundos()));
    }

    private TentativaSeguidorLinhaDTO base(int tomada, int tentativa) {
        TentativaSeguidorLinhaDTO dto = new TentativaSeguidorLinhaDTO();
        dto.setRegistrationId(8L);
        dto.setTomada(tomada);
        dto.setNumeroTentativa(tentativa);
        dto.setTempoSegundos(new BigDecimal("45.000"));
        dto.setCheckpointsAlcancados(5);
        dto.setPenalidadeSegundos(0);
        dto.setConcluida(true);
        dto.setValida(true);
        return dto;
    }
}
