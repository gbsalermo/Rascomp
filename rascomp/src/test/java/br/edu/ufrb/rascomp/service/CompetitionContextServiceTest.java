package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;

@ExtendWith(MockitoExtension.class)
class CompetitionContextServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private CompetitionContextService service;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devDeveVerTodasAsEdicoes() {
        autenticar(UserRole.DEV);
        when(competitionRepository.findAllByOrderByDataInicioDesc()).thenReturn(List.of(
                competition(1L, StatusCompetition.FINALIZADA),
                competition(2L, StatusCompetition.EM_ANDAMENTO)));

        var result = service.listarVisiveis(false);

        assertEquals(2, result.size());
    }

    @Test
    void gestaoDeveVerSomenteACompeticaoVigente() {
        autenticar(UserRole.GESTAO);
        Competition vigente = competition(3L, StatusCompetition.PLANEJADA);
        vigente.setVigente(true);
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue()).thenReturn(Optional.of(vigente));

        var result = service.listarVisiveis(false);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void gestaoNaoDeveOperarEdicaoForaDaVigente() {
        Competition antiga = competition(1L, StatusCompetition.FINALIZADA);
        Competition vigente = competition(2L, StatusCompetition.EM_ANDAMENTO);

        autenticar(UserRole.GESTAO);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(antiga));
        vigente.setVigente(true);
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue()).thenReturn(Optional.of(vigente));

        assertThrows(AccessDeniedException.class, () -> service.exigirOperavel(1L));
    }

    @Test
    void devPodeOperarEdicaoHistorica() {
        Competition antiga = competition(1L, StatusCompetition.FINALIZADA);

        autenticar(UserRole.DEV);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(antiga));

        assertEquals(1L, service.exigirOperavel(1L).getId());
    }

    @Test
    void vigenteDeveSerAEdicaoMarcadaExplicitamenteMesmoSePlanejada() {
        autenticar(UserRole.GESTAO);
        Competition vigente = competition(12L, StatusCompetition.PLANEJADA);
        vigente.setVigente(true);
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue()).thenReturn(Optional.of(vigente));

        var result = service.buscarVigente();

        assertEquals(12L, result.getId());
    }

    @Test
    void devDeveDefinirNovaCompeticaoVigente() {
        autenticar(UserRole.DEV);
        Competition target = competition(20L, StatusCompetition.PLANEJADA);
        when(competitionRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(target));
        when(competitionRepository.save(target)).thenReturn(target);

        var result = service.definirVigente(20L);

        assertEquals(20L, result.getId());
        assertEquals(true, result.getVigente());
    }

    @Test
    void gestaoNaoPodeFinalizarCompeticao() {
        autenticar(UserRole.GESTAO);
        Competition vigente = competition(30L, StatusCompetition.EM_ANDAMENTO);
        vigente.setVigente(true);
        when(competitionRepository.findById(30L)).thenReturn(Optional.of(vigente));
        when(competitionRepository.findFirstByVigenteTrueAndAtivoTrue()).thenReturn(Optional.of(vigente));

        assertThrows(AccessDeniedException.class,
                () -> service.exigirPodeAlterarStatus(30L, StatusCompetition.FINALIZADA));
    }

    private void autenticar(UserRole role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "operador@rascomp.local",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
