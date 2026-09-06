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

import br.edu.ufrb.rascomp.dto.ConfigFollowDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;

@ExtendWith(MockitoExtension.class)
class ConfigFollowServiceTest {

    @Mock private ConfigFollowRepository configFollowRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;

    @InjectMocks private ConfigFollowService service;

    private CompetitionCategory category;

    @BeforeEach
    void setup() {
        category = CompetitionCategory.builder()
                .id(10L)
                .nome("Seguidor de Linha")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
    }

    @Test
    void deveFixarEstruturaRrcEmTresPorTres() {
        ConfigFollowDTO dto = configBase();
        when(configFollowRepository.existsByCompetitionCategoryId(10L)).thenReturn(false);
        when(configFollowRepository.save(any(ConfigFollow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigFollowDTO result = service.criar(10L, dto);

        assertEquals(3, result.getNumeroTomadas());
        assertEquals(3, result.getTentativasPorTomada());
    }

    @Test
    void deveRejeitarEstruturaDiferenteDeTresPorTres() {
        ConfigFollowDTO dto = configBase();
        dto.setNumeroTomadas(2);
        when(configFollowRepository.existsByCompetitionCategoryId(10L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.criar(10L, dto));
        verify(configFollowRepository, never()).save(any());
    }

    @Test
    void deveAplicarPadroesOperacionaisQuandoNaoInformados() {
        ConfigFollowDTO dto = configBase();
        dto.setPenalidadePadraoSegundos(null);
        dto.setTempoApresentacaoSegundos(null);
        when(configFollowRepository.existsByCompetitionCategoryId(10L)).thenReturn(false);
        when(configFollowRepository.save(any(ConfigFollow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigFollowDTO result = service.criar(10L, dto);

        assertEquals(10, result.getPenalidadePadraoSegundos());
        assertEquals(60, result.getTempoApresentacaoSegundos());
    }

    private ConfigFollowDTO configBase() {
        ConfigFollowDTO dto = new ConfigFollowDTO();
        dto.setNumeroTomadas(3);
        dto.setTentativasPorTomada(3);
        dto.setMaxTempoSegundos(120);
        dto.setNumeroCheckpoints(5);
        dto.setPenalidadePadraoSegundos(10);
        dto.setTempoApresentacaoSegundos(60);
        return dto;
    }
}
