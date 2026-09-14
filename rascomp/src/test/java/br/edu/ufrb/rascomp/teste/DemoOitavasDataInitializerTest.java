package br.edu.ufrb.rascomp.teste;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import br.edu.ufrb.rascomp.service.RoundSumoService;

@ExtendWith(MockitoExtension.class)
class DemoOitavasDataInitializerTest {

    @Mock private CompetitionRepository competitionRepository;
    @Mock private CompetitionCategoryRepository categoryRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private BracketRepository bracketRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private RoundSumoRepository roundRepository;
    @Mock private InspecaoSumoService inspecaoSumoService;
    @Mock private BracketGenerationService bracketGenerationService;
    @Mock private RoundSumoService roundSumoService;

    @InjectMocks
    private DemoOitavasDataInitializer initializer;

    @Test
    void bancoLocalReaproveitadoNaoDeveRegenerarChaveComCompeticaoEmAndamento() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setNome("RRC 2026 · Demonstração ao vivo");
        competition.setStatus(StatusCompetition.EM_ANDAMENTO);

        CompetitionCategory category = CompetitionCategory.builder()
                .id(2L)
                .nome("DEMO · Mini Sumô RC")
                .build();

        Institution institution = new Institution();
        institution.setSigla("ROBODEMO");

        UserAccount organizacao = new UserAccount();

        when(competitionRepository.findAll()).thenReturn(List.of(competition));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(institutionRepository.findAll()).thenReturn(List.of(institution));
        when(userAccountRepository.findByEmailIgnoreCase("organizacao.demo@rascomp.local"))
                .thenReturn(Optional.of(organizacao));
        when(bracketRepository.findByCompetitionIdAndCategoryIdAndAtualTrue(1L, 2L))
                .thenReturn(List.of());

        initializer.prepararOitavasAoVivo();

        verify(bracketGenerationService, never()).gerar(anyLong(), anyLong());
    }
}
