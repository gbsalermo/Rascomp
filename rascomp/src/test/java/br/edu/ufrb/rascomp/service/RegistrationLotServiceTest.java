package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.RegistrationLotDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.RegistrationLot;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationLotRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationLotServiceTest {

    @Mock private RegistrationLotRepository repository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionContextService competitionContextService;

    @InjectMocks
    private RegistrationLotService service;

    @Test
    void deveCriarLoteDentroDaJanelaSemSobreposicao() {
        Competition competition = competition();
        RegistrationLotDTO dto = dto("1º lote", LocalDate.now(), LocalDate.now().plusDays(4));

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(repository.findByCompetitionIdAndAtivoTrueOrderByDataInicioAscIdAsc(1L))
                .thenReturn(List.of());
        when(repository.save(any(RegistrationLot.class))).thenAnswer(invocation -> {
            RegistrationLot lot = invocation.getArgument(0);
            lot.setId(10L);
            return lot;
        });

        RegistrationLotDTO result = service.criar(1L, dto);

        assertEquals(10L, result.getId());
        assertEquals("1º lote", result.getNome());
    }

    @Test
    void naoDeveAceitarLotesSobrepostos() {
        Competition competition = competition();
        RegistrationLot existente = new RegistrationLot();
        existente.setId(9L);
        existente.setCompetition(competition);
        existente.setNome("1º lote");
        existente.setDataInicio(LocalDate.now());
        existente.setDataFim(LocalDate.now().plusDays(4));
        existente.setAtivo(true);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(repository.findByCompetitionIdAndAtivoTrueOrderByDataInicioAscIdAsc(1L))
                .thenReturn(List.of(existente));

        RegistrationLotDTO dto =
                dto("2º lote", LocalDate.now().plusDays(3), LocalDate.now().plusDays(6));

        assertThrows(IllegalArgumentException.class, () -> service.criar(1L, dto));
    }

    @Test
    void deveManterFluxoSemLoteQuandoCompeticaoNaoUsaLotes() {
        Competition competition = competition();
        when(repository.existsByCompetitionIdAndAtivoTrue(1L)).thenReturn(false);

        assertNull(service.resolverParaNovaInscricao(competition));
    }

    @Test
    void deveExigirLoteVigenteQuandoExistiremLotesConfigurados() {
        Competition competition = competition();
        LocalDate hoje = LocalDate.now();

        when(repository.existsByCompetitionIdAndAtivoTrue(1L)).thenReturn(true);
        when(repository
                .findFirstByCompetitionIdAndAtivoTrueAndDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
                        1L, hoje, hoje))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolverParaNovaInscricao(competition));
    }

    private Competition competition() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC");
        competition.setInicioInscricoes(LocalDate.now().minusDays(2));
        competition.setFimInscricoes(LocalDate.now().plusDays(10));
        competition.setDataInicio(LocalDate.now().plusDays(11));
        competition.setDataFim(LocalDate.now().plusDays(12));
        competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        competition.setAtivo(true);
        return competition;
    }

    private RegistrationLotDTO dto(String nome, LocalDate inicio, LocalDate fim) {
        RegistrationLotDTO dto = new RegistrationLotDTO();
        dto.setNome(nome);
        dto.setDataInicio(inicio);
        dto.setDataFim(fim);
        return dto;
    }
}
