package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock private BracketRepository bracketRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;

    @InjectMocks
    private BracketService service;

    @Test
    void deveRejeitarCriacaoDeBracketParaFollowLine() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setAtivo(true);

        CompetitionCategory follow = CompetitionCategory.builder()
                .id(3L)
                .nome("Seguidor de Linha")
                .modalidade(Modalidade.FOLLOW_LINE)
                .ativo(true)
                .build();

        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(follow));

        BracketDTO dto = new BracketDTO();
        dto.setCompetitionId(1L);
        dto.setCategoryId(3L);
        dto.setNome("Chave inválida");

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(bracketRepository);
    }

    @Test
    void devePreservarBracketAnteriorComoHistoricoAoCriarNovoAtual() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC 2026");
        competition.setAtivo(true);

        CompetitionCategory sumo = CompetitionCategory.builder()
                .id(2L)
                .nome("Sumô 3kg")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        Bracket anterior = new Bracket();
        anterior.setId(10L);
        anterior.setCompetition(competition);
        anterior.setCategory(sumo);
        anterior.setNome("Chave anterior");
        anterior.setAtivo(true);
        anterior.setAtual(true);

        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(sumo));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(1L, 2L))
                .thenReturn(List.of(anterior));
        when(bracketRepository.save(any(Bracket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BracketDTO dto = new BracketDTO();
        dto.setCompetitionId(1L);
        dto.setCategoryId(2L);
        dto.setNome("Nova chave");

        BracketDTO criado = service.criar(dto);

        assertFalse(anterior.getAtual());
        assertTrue(criado.getAtual());
        verify(bracketRepository).saveAll(List.of(anterior));
    }
}
