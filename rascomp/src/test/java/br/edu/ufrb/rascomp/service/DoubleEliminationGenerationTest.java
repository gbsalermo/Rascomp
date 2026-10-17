package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.*;
import br.edu.ufrb.rascomp.model.Enum.*;
import br.edu.ufrb.rascomp.repository.*;

@ExtendWith(MockitoExtension.class)
class DoubleEliminationGenerationTest {
    @Mock BracketRepository bracketRepository;
    @Mock MatchRepository matchRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock CompetitionRepository competitionRepository;
    @Mock CompetitionCategoryRepository categoryRepository;
    @Mock BracketProgressionService bracketProgressionService;
    @Mock BracketIntegrityService bracketIntegrityService;
    @Mock InspecaoSumoService inspecaoSumoService;
    @Mock CompetitionContextService competitionContextService;
    @Mock UserAccountService userAccountService;
    @InjectMocks BracketGenerationService service;

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 8, 9})
    void geraWinnersLosersEFinaisSemSobreposicaoEComByes(int quantity) {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("Competição");
        competition.setAtivo(true);
        competition.setStatus(StatusCompetition.INSCRICOES_ENCERRADAS);
        CompetitionCategory category = CompetitionCategory.builder()
                .id(2L).nome("Sumô").modalidade(Modalidade.SUMO).ativo(true).build();

        List<Registration> registrations = IntStream.rangeClosed(1, quantity).mapToObj(index -> {
            Registration r = new Registration();
            r.setId((long) index);
            r.setCompetition(competition);
            r.setCategory(category);
            r.setAtivo(true);
            r.setStatus(StatusRegistration.APROVADA);
            return r;
        }).toList();

        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(registrationRepository.findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                1L, 2L, StatusRegistration.APROVADA)).thenReturn(registrations);
        registrations.forEach(r -> when(inspecaoSumoService.estaAptaParaCompetir(r.getId())).thenReturn(true));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(1L, 2L)).thenReturn(List.of());
        when(bracketRepository.save(any(Bracket.class))).thenAnswer(call -> {
            Bracket b = call.getArgument(0);
            b.setId(50L);
            return b;
        });

        AtomicLong ids = new AtomicLong();
        List<Match> saved = new ArrayList<>();
        when(matchRepository.saveAll(any())).thenAnswer(call -> {
            Iterable<Match> batch = call.getArgument(0);
            saved.clear();
            for (Match m : batch) {
                if (m.getId() == null) m.setId(ids.incrementAndGet());
                saved.add(m);
            }
            return batch;
        });

        var dto = service.gerar(1L, 2L, "DOUBLE_ELIMINATION");
        int size = 1;
        while (size < quantity) size *= 2;

        assertEquals("DOUBLE_ELIMINATION", dto.getBracketFormat());
        assertEquals(2 * size - 1, saved.size()); // Winners (size-1), Losers (size-2), two finals
        assertEquals(size - 1, saved.stream().filter(m -> "WINNERS".equals(m.getBracketSection())).count());
        assertEquals(size - 2, saved.stream().filter(m -> "LOSERS".equals(m.getBracketSection())).count());
        assertEquals(1, saved.stream().filter(m -> "GRAND_FINAL".equals(m.getBracketSection())).count());
        assertEquals(1, saved.stream().filter(m -> "GRAND_FINAL_RESET".equals(m.getBracketSection())).count());

        HashSet<String> coordinates = new HashSet<>();
        for (Match m : saved) {
            assertTrue(coordinates.add(m.getRodada() + ":" + m.getOrdem()), "Rodada/ordem duplicada");
            if (m.getWinnerNextMatch() != null)
                assertTrue(m.getWinnerNextMatch().getRodada() > m.getRodada(), "Winner deve avançar");
            if (m.getLoserNextMatch() != null)
                assertTrue(m.getLoserNextMatch().getRodada() > m.getRodada(), "Loser deve avançar");
            if ("WINNERS".equals(m.getBracketSection()))
                assertNotNull(m.getLoserNextMatch(), "Toda primeira derrota precisa de destino");
        }
        assertEquals(size - quantity,
                saved.stream().filter(m -> m.getRodada() == 1 && m.getStatus() == StatusMatch.BYE).count());
        verify(bracketProgressionService).resolverAvancosAutomaticos(50L);
    }
}
