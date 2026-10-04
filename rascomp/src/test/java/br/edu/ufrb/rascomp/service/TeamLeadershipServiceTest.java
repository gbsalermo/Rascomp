package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.TeamLeaderTransferRequest;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TeamLeadershipHistory;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.TeamLeadershipHistoryRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamLeadershipServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private ParticipantCompetitionRegistrationRepository participantRegistrationRepository;
    @Mock private TeamLeadershipHistoryRepository historyRepository;
    @Mock private UserAccountService userAccountService;

    @InjectMocks
    private TeamLeadershipService service;

    @Test
    void devPodeTransferirLiderancaParaParticipantePendenteDaMesmaEquipe() {
        UserAccount dev = user(1L, "DEV", UserRole.DEV);
        UserAccount anterior = user(2L, "Anterior", UserRole.PARTICIPANTE);
        UserAccount novo = user(3L, "Novo", UserRole.PARTICIPANTE);

        Team team = new Team();
        team.setId(10L);
        team.setNome("Equipe");
        team.setResponsibleUser(anterior);
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(20L);
        competitor.setNome("Novo");
        competitor.setTeam(team);
        competitor.setUserAccount(novo);
        competitor.setAtivo(true);

        Competition competition = new Competition();
        competition.setId(30L);
        competition.setNome("RRC");

        ParticipantCompetitionRegistration pessoal = new ParticipantCompetitionRegistration();
        pessoal.setCompetition(competition);
        pessoal.setCompetitor(competitor);
        pessoal.setStatus(ParticipantCompetitionRegistrationStatus.PENDENTE);
        pessoal.setAtivo(true);

        TeamLeaderTransferRequest request = new TeamLeaderTransferRequest();
        request.setCompetitionId(30L);
        request.setNewResponsibleUserId(3L);
        request.setMotivo("Substituição de liderança para continuidade da equipe.");

        when(userAccountService.buscarAtual()).thenReturn(dev);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(competitionRepository.findById(30L)).thenReturn(Optional.of(competition));
        when(competitorRepository.findByUserAccountId(3L)).thenReturn(Optional.of(competitor));
        when(participantRegistrationRepository.findByCompetitionIdAndCompetitorId(30L, 20L))
                .thenReturn(Optional.of(pessoal));
        when(teamRepository.save(team)).thenReturn(team);
        when(historyRepository.save(any(TeamLeadershipHistory.class)))
                .thenAnswer(invocation -> {
                    TeamLeadershipHistory history = invocation.getArgument(0);
                    history.setId(40L);
                    return history;
                });

        var result = service.transferir(10L, request);

        assertEquals(3L, team.getResponsibleUser().getId());
        assertEquals(2L, result.getPreviousUserId());
        assertEquals(3L, result.getNewUserId());
        assertEquals(1L, result.getChangedByUserId());
    }

    @Test
    void gestaoNaoPodeTransferirLideranca() {
        UserAccount gestao = user(1L, "Gestão", UserRole.GESTAO);
        when(userAccountService.buscarAtual()).thenReturn(gestao);

        TeamLeaderTransferRequest request = new TeamLeaderTransferRequest();
        request.setCompetitionId(30L);
        request.setNewResponsibleUserId(3L);
        request.setMotivo("Tentativa inválida.");

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> service.transferir(10L, request));
    }

    private UserAccount user(Long id, String nome, UserRole role) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(nome.toLowerCase() + "@teste.local");
        user.setRole(role);
        user.setAtivo(true);
        return user;
    }
}
