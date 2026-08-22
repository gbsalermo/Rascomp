package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.CompetitionCategoryDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CompetitionCategoryServiceTest {

    @Mock
    private CompetitionCategoryRepository competitionCategoryRepository;

    @InjectMocks
    private CompetitionCategoryService service;

    @Test
    void criarDeveAssumirAtivoTrueQuandoAtivoNaoForInformado() {
        CompetitionCategoryDTO dto = new CompetitionCategoryDTO();
        dto.setNome("Mini Sumo Teste");
        dto.setDescricao("Categoria automatizada");
        dto.setModalidade(Modalidade.SUMO);
        dto.setAtivo(null);

        when(competitionCategoryRepository.save(any(CompetitionCategory.class)))
                .thenAnswer(invocation -> {
                    CompetitionCategory entity = invocation.getArgument(0);
                    entity.setId(10L);
                    return entity;
                });

        CompetitionCategoryDTO resultado = service.criar(dto);

        assertEquals(10L, resultado.getId());
        assertEquals("Mini Sumo Teste", resultado.getNome());
        assertEquals(Modalidade.SUMO, resultado.getModalidade());
        assertTrue(resultado.getAtivo());
    }

    @Test
    void listarTodosDeveMapearEntidadesParaDto() {
        CompetitionCategory categoria = CompetitionCategory.builder()
                .id(3L)
                .nome("Seguidor de Linha")
                .descricao("Follow line")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        when(competitionCategoryRepository.findAll()).thenReturn(List.of(categoria));

        List<CompetitionCategoryDTO> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(3L, resultado.get(0).getId());
        assertEquals("Seguidor de Linha", resultado.get(0).getNome());
        assertEquals(Modalidade.FOLLOW_LINE, resultado.get(0).getModalidade());
    }

    @Test
    void buscarPorIdDeveFalharQuandoCategoriaNaoExistir() {
        when(competitionCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(999L));
    }
}
