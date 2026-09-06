package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionLifecycleServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private CompetitionService service;

    @Test
    void novaCompeticaoDeveNascerPlanejada() {
        CompetitionDTO dto = dto(StatusCompetition.PLANEJADA);
        when(competitionRepository.save(any(Competition.class))).thenAnswer(invocation -> {
            Competition entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        CompetitionDTO result = service.criar(dto);

        assertEquals(StatusCompetition.PLANEJADA, result.getStatus());
    }

    @Test
    void novaCompeticaoNaoPodeNascerEmAndamento() {
        CompetitionDTO dto = dto(StatusCompetition.EM_ANDAMENTO);

        assertThrows(IllegalArgumentException.class, () -> service.criar(dto));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void devePermitirAvancoPlanejadaParaInscricoesAbertas() {
        Competition competition = competition(StatusCompetition.PLANEJADA);
        CompetitionDTO dto = dto(StatusCompetition.INSCRICOES_ABERTAS);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(competitionRepository.save(any(Competition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompetitionDTO result = service.atualizar(1L, dto);

        assertEquals(StatusCompetition.INSCRICOES_ABERTAS, result.getStatus());
    }

    @Test
    void naoDevePularDeInscricoesAbertasParaEmAndamento() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        CompetitionDTO dto = dto(StatusCompetition.EM_ANDAMENTO);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(1L, dto));
        assertEquals(StatusCompetition.INSCRICOES_ABERTAS, competition.getStatus());
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void competicaoFinalizadaNaoPodeReabrirPeloFluxoComum() {
        Competition competition = competition(StatusCompetition.FINALIZADA);
        CompetitionDTO dto = dto(StatusCompetition.INSCRICOES_ABERTAS);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(1L, dto));
        assertEquals(StatusCompetition.FINALIZADA, competition.getStatus());
        verify(competitionRepository, never()).save(any());
    }

    private CompetitionDTO dto(StatusCompetition status) {
        CompetitionDTO dto = new CompetitionDTO();
        dto.setNome("RRC Teste");
        dto.setDescricao("Fluxo de integridade");
        dto.setInicioInscricoes(LocalDate.now().minusDays(5));
        dto.setFimInscricoes(LocalDate.now().plusDays(5));
        dto.setDataInicio(LocalDate.now().plusDays(10));
        dto.setDataFim(LocalDate.now().plusDays(11));
        dto.setStatus(status);
        dto.setAtivo(true);
        return dto;
    }

    private Competition competition(StatusCompetition status) {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC Teste");
        competition.setDescricao("Fluxo de integridade");
        competition.setInicioInscricoes(LocalDate.now().minusDays(5));
        competition.setFimInscricoes(LocalDate.now().plusDays(5));
        competition.setDataInicio(LocalDate.now().plusDays(10));
        competition.setDataFim(LocalDate.now().plusDays(11));
        competition.setStatus(status);
        competition.setAtivo(true);
        return competition;
    }
}
