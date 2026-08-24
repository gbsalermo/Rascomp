package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.BracketDTO;
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

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(follow));

        BracketDTO dto = new BracketDTO();
        dto.setCompetitionId(1L);
        dto.setCategoryId(3L);
        dto.setNome("Chave inválida");

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verifyNoInteractions(bracketRepository);
    }
}
