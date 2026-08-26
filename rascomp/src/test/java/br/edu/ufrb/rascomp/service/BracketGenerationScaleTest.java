package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;

@ExtendWith(MockitoExtension.class)
class BracketGenerationScaleTest {

    @Mock private BracketRepository bracketRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private BracketProgressionService bracketProgressionService;
    @Mock private InspecaoSumoService inspecaoSumoService;

    @InjectMocks private BracketGenerationService service;

    private Competition competition;
    private CompetitionCategory category;
    private List<Match> saved;

    @BeforeEach
    void setup() {
        competition = new Competition();
        competition.setId(10L);
        competition.setNome("RRC Escala");
        competition.setAtivo(true);

        category = CompetitionCategory.builder()
                .id(20L)
                .nome("Mini Sumô")
                .modalidade(Modalidade.SUMO)
                .ativo(true)
                .build();

        when(competitionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(competition));
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(10L, 20L)).thenReturn(List.of());
        when(bracketRepository.save(any(Bracket.class))).thenAnswer(invocation -> {
            Bracket bracket = invocation.getArgument(0);
            if (bracket.getId() == null) bracket.setId(100L);
            return bracket;
        });

        saved = new ArrayList<>();
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Match> matches = invocation.getArgument(0);
            matches.forEach(saved::add);
            return saved;
        });
    }

    @Test
    void trintaEDoisParticipantesDevemGerar16AvosSemBye() {
        List<Registration> registrations = registrations(32);
        stubEligible(registrations);

        service.gerar(10L, 20L);

        assertEquals(31, saved.size(), "Chave de 32 deve possuir 31 partidas no total");
        assertEquals(16, countRound(1));
        assertEquals(8, countRound(2));
        assertEquals(4, countRound(3));
        assertEquals(2, countRound(4));
        assertEquals(1, countRound(5));
        assertEquals(0, saved.stream().filter(m -> m.getStatus() == StatusMatch.BYE).count());
        verify(bracketProgressionService, never()).avancarBye(any(Match.class));
    }

    @Test
    void dezParticipantesDevemGerarChave16ComSeisByes() {
        List<Registration> registrations = registrations(10);
        stubEligible(registrations);

        service.gerar(10L, 20L);

        assertEquals(15, saved.size());
        assertEquals(8, countRound(1));
        assertEquals(6, saved.stream().filter(m -> m.getStatus() == StatusMatch.BYE).count());
        assertEquals(2, saved.stream().filter(m -> m.getRodada() == 1 && m.getStatus() == StatusMatch.AGENDADA).count());
        verify(bracketProgressionService, org.mockito.Mockito.times(6)).avancarBye(any(Match.class));
    }

    private long countRound(int round) {
        return saved.stream().filter(m -> m.getRodada() == round).count();
    }

    private List<Registration> registrations(int total) {
        List<Registration> result = new ArrayList<>();
        for (long i = 1; i <= total; i++) {
            Registration registration = new Registration();
            registration.setId(i);
            registration.setCompetition(competition);
            registration.setCategory(category);
            registration.setStatus(StatusRegistration.APROVADA);
            registration.setAtivo(true);
            result.add(registration);
        }
        return result;
    }

    private void stubEligible(List<Registration> registrations) {
        when(registrationRepository.findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                eq(10L), eq(20L), eq(StatusRegistration.APROVADA)))
                .thenReturn(registrations);
        registrations.forEach(registration ->
                when(inspecaoSumoService.estaAptaParaCompetir(registration.getId())).thenReturn(true));
    }
}
