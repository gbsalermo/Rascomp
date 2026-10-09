package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private RegistrationCompositionService registrationCompositionService;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private CompetitionService service;

    @Test
    void naoDeveIniciarCompeticaoComInscricoesAbertas() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ABERTAS);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarStatus(1L, StatusCompetition.EM_ANDAMENTO));

        assertEquals(true, ex.getMessage().contains("Transição de competição inválida"));
        verify(registrationCompositionService, never())
                .consolidarPendentesDaCompeticao(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveConsolidarMudancasAoIniciarDepoisDeEncerrarInscricoes() {
        Competition competition = competition(StatusCompetition.INSCRICOES_ENCERRADAS);
        UserAccount actor = new UserAccount();
        actor.setId(9L);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(userAccountService.buscarAtual()).thenReturn(actor);
        when(competitionRepository.save(competition)).thenReturn(competition);

        var result = service.alterarStatus(1L, StatusCompetition.EM_ANDAMENTO);

        assertEquals(StatusCompetition.EM_ANDAMENTO, result.getStatus());
        verify(registrationCompositionService)
                .consolidarPendentesDaCompeticao(1L, actor);
    }

    private Competition competition(StatusCompetition status) {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setInicioInscricoes(LocalDate.now().minusDays(10));
        competition.setFimInscricoes(LocalDate.now().minusDays(1));
        competition.setDataInicio(LocalDate.now().plusDays(1));
        competition.setDataFim(LocalDate.now().plusDays(3));
        competition.setStatus(status);
        competition.setAtivo(true);
        return competition;
    }

    @Test
    void finalizarCompeticaoDeveRemoverMarcacaoVigente() {
        Competition competition = competition(StatusCompetition.EM_ANDAMENTO);
        competition.setVigente(true);
        when(competitionRepository.findById(1L)).thenReturn(java.util.Optional.of(competition));
        when(competitionRepository.save(competition)).thenReturn(competition);

        var result = service.alterarStatus(1L, StatusCompetition.FINALIZADA);

        assertEquals(StatusCompetition.FINALIZADA, result.getStatus());
        assertEquals(false, result.getVigente());
    }

    @Test
    void desativarCompeticaoDeveRemoverMarcacaoVigente() {
        Competition competition = competition(StatusCompetition.PLANEJADA);
        competition.setVigente(true);
        when(competitionRepository.findById(1L)).thenReturn(java.util.Optional.of(competition));

        service.deletar(1L);

        assertEquals(false, competition.getAtivo());
        assertEquals(false, competition.getVigente());
        verify(competitionRepository).save(competition);
    }

}
