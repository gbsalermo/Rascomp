package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class PublicQueryCompetitionVisibilityTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private PublicQueryService service;

    @Test
    void semVigenteNaoDevePublicarCompeticao() {
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue()).thenReturn(Optional.empty());

        assertEquals(0, service.competicoes().size());
    }

    @Test
    void planejadaMesmoVigenteNaoDeveSerEspelhadaNaLanding() {
        Competition competition = competition(StatusCompetition.PLANEJADA);
        competition.setVigente(true);
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue())
                .thenReturn(Optional.of(competition));

        assertEquals(0, service.competicoes().size());
    }

    @Test
    void vigenteComStatusPublicoDeveSerPublicada() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setVigente(true);
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue())
                .thenReturn(Optional.of(competition));

        var result = service.competicoes();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    private Competition competition(StatusCompetition status) {
        Competition competition = new Competition();
        competition.setId(10L);
        competition.setNome("RRC");
        competition.setInicioInscricoes(LocalDate.now().minusDays(1));
        competition.setFimInscricoes(LocalDate.now().plusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(2));
        competition.setDataFim(LocalDate.now().plusDays(3));
        competition.setStatus(status);
        competition.setAtivo(true);
        return competition;
    }
}
