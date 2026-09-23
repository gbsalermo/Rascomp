package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionContextServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private CompetitionContextService service;

    @Test
    void devDeveVerTodasAsEdicoes() {
        when(userAccountService.buscarAtual()).thenReturn(usuario(UserRole.DEV));
        when(competitionRepository.findAllByOrderByDataInicioDesc()).thenReturn(List.of(
                competition(1L, StatusCompetition.FINALIZADA),
                competition(2L, StatusCompetition.EM_ANDAMENTO)));

        var result = service.listarVisiveis(false);

        assertEquals(2, result.size());
    }

    @Test
    void gestaoDeveVerSomenteACompeticaoVigente() {
        when(userAccountService.buscarAtual()).thenReturn(usuario(UserRole.GESTAO));
        when(competitionRepository.findByAtivoTrueOrderByDataInicioDesc()).thenReturn(List.of(
                competition(1L, StatusCompetition.PLANEJADA),
                competition(2L, StatusCompetition.INSCRICOES_ENCERRADAS),
                competition(3L, StatusCompetition.EM_ANDAMENTO)));

        var result = service.listarVisiveis(false);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void gestaoNaoDeveOperarEdicaoForaDaVigente() {
        Competition antiga = competition(1L, StatusCompetition.FINALIZADA);
        Competition vigente = competition(2L, StatusCompetition.EM_ANDAMENTO);

        when(userAccountService.buscarAtual()).thenReturn(usuario(UserRole.GESTAO));
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(antiga));
        when(competitionRepository.findByAtivoTrueOrderByDataInicioDesc()).thenReturn(List.of(vigente));

        assertThrows(AccessDeniedException.class, () -> service.exigirOperavel(1L));
    }

    @Test
    void devPodeOperarEdicaoHistorica() {
        Competition antiga = competition(1L, StatusCompetition.FINALIZADA);

        when(userAccountService.buscarAtual()).thenReturn(usuario(UserRole.DEV));
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(antiga));

        assertEquals(1L, service.exigirOperavel(1L).getId());
    }

    @Test
    void prioridadeDaVigenteDeveSerAndamentoDepoisInscricoesAbertasEncerradasEPlanejada() {
        when(userAccountService.buscarAtual()).thenReturn(usuario(UserRole.GESTAO));
        when(competitionRepository.findByAtivoTrueOrderByDataInicioDesc()).thenReturn(List.of(
                competition(10L, StatusCompetition.PLANEJADA),
                competition(11L, StatusCompetition.INSCRICOES_ENCERRADAS),
                competition(12L, StatusCompetition.INSCRICOES_ABERTAS)));

        var result = service.buscarVigente();

        assertEquals(12L, result.getId());
    }

    private UserAccount usuario(UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(99L);
        user.setNome("Operador");
        user.setEmail("operador@rascomp.local");
        user.setRole(role);
        user.setAtivo(true);
        return user;
    }

    private Competition competition(Long id, StatusCompetition status) {
        Competition item = new Competition();
        item.setId(id);
        item.setNome("RRC " + id);
        item.setInicioInscricoes(LocalDate.now().minusDays(20));
        item.setFimInscricoes(LocalDate.now().minusDays(10));
        item.setDataInicio(LocalDate.now().minusDays(2));
        item.setDataFim(LocalDate.now().plusDays(2));
        item.setStatus(status);
        item.setAtivo(true);
        return item;
    }
}
