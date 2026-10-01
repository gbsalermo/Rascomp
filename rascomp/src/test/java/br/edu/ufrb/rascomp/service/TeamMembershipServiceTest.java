package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ufrb.rascomp.dto.TeamInviteCreateRequest;
import br.edu.ufrb.rascomp.dto.TeamJoinCreateRequest;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.TeamMembershipRequest;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipRequestType;
import br.edu.ufrb.rascomp.model.Enum.TeamMembershipStatus;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.TeamMembershipRequestRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TeamMembershipServiceTest {

    @Mock private AccessPolicyService accessPolicyService;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private TeamMembershipRequestRepository requestRepository;

    @InjectMocks
    private TeamMembershipService service;

    private UserAccount lider;
    private UserAccount participante;
    private Team team;

    @BeforeEach
    void setUp() {
        lider = user(1L, "Líder", "lider@rascomp.local");
        participante = user(2L, "Participante", "participante@rascomp.local");

        Institution institution = new Institution();
        institution.setId(3L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);

        team = new Team();
        team.setId(4L);
        team.setNome("Equipe Teste");
        team.setInstitution(institution);
        team.setResponsibleUser(lider);
        team.setAtivo(true);
    }

    @Test
    void liderPodeConvidarContaParticipanteSemEquipe() {
        when(accessPolicyService.exigirEquipeDoResponsavel(4L)).thenReturn(team);
        when(accessPolicyService.usuarioAtual()).thenReturn(lider);
        when(userAccountRepository.findByEmailIgnoreCase("participante@rascomp.local"))
                .thenReturn(Optional.of(participante));
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.empty());
        when(requestRepository.existsByTeamIdAndParticipantUserIdAndStatus(
                4L, 2L, TeamMembershipStatus.PENDENTE)).thenReturn(false);
        when(requestRepository.save(any(TeamMembershipRequest.class)))
                .thenAnswer(invocation -> {
                    TeamMembershipRequest entity = invocation.getArgument(0);
                    entity.setId(10L);
                    return entity;
                });

        TeamInviteCreateRequest request = new TeamInviteCreateRequest();
        request.setEmail("participante@rascomp.local");
        request.setMensagem("Venha para nossa equipe.");

        var result = service.convidar(4L, request);

        assertEquals(10L, result.getId());
        assertEquals(TeamMembershipRequestType.CONVITE, result.getRequestType());
        assertEquals(TeamMembershipStatus.PENDENTE, result.getStatus());
        assertEquals(2L, result.getParticipantUserId());
    }

    @Test
    void participantePodeSolicitarEntrada() {
        when(accessPolicyService.usuarioAtual()).thenReturn(participante);
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.empty());
        when(teamRepository.findById(4L)).thenReturn(Optional.of(team));
        when(requestRepository.existsByTeamIdAndParticipantUserIdAndStatus(
                4L, 2L, TeamMembershipStatus.PENDENTE)).thenReturn(false);
        when(requestRepository.save(any(TeamMembershipRequest.class)))
                .thenAnswer(invocation -> {
                    TeamMembershipRequest entity = invocation.getArgument(0);
                    entity.setId(11L);
                    return entity;
                });

        var result = service.solicitarEntrada(4L, new TeamJoinCreateRequest());

        assertEquals(TeamMembershipRequestType.SOLICITACAO, result.getRequestType());
        assertEquals(TeamMembershipStatus.PENDENTE, result.getStatus());
    }

    @Test
    void aceitarConviteCriaCompetidorVinculadoAContaEEquipe() {
        TeamMembershipRequest invite = membership(
                20L,
                TeamMembershipRequestType.CONVITE,
                participante,
                lider);

        when(requestRepository.findById(20L)).thenReturn(Optional.of(invite));
        when(accessPolicyService.usuarioAtual()).thenReturn(participante);
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.empty());
        when(competitorRepository.findByEmailIgnoreCase(participante.getEmail())).thenReturn(Optional.empty());
        when(competitorRepository.save(any(Competitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(any(TeamMembershipRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.aceitarConvite(20L);

        ArgumentCaptor<Competitor> captor = ArgumentCaptor.forClass(Competitor.class);
        verify(competitorRepository).save(captor.capture());

        assertEquals(4L, captor.getValue().getTeam().getId());
        assertEquals(2L, captor.getValue().getUserAccount().getId());
        assertEquals(TeamMembershipStatus.ACEITA, result.getStatus());
    }

    @Test
    void contaJaAssociadaNaoPodeSolicitarOutraEquipe() {
        when(accessPolicyService.usuarioAtual()).thenReturn(participante);

        Competitor existing = new Competitor();
        existing.setId(30L);
        existing.setTeam(team);
        existing.setUserAccount(participante);
        existing.setAtivo(true);
        when(competitorRepository.findByUserAccountId(2L)).thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.solicitarEntrada(99L, new TeamJoinCreateRequest()));
    }

    private TeamMembershipRequest membership(
            Long id,
            TeamMembershipRequestType type,
            UserAccount participant,
            UserAccount requestedBy) {
        TeamMembershipRequest request = new TeamMembershipRequest();
        request.setId(id);
        request.setTeam(team);
        request.setParticipantUser(participant);
        request.setRequestedByUser(requestedBy);
        request.setRequestType(type);
        request.setStatus(TeamMembershipStatus.PENDENTE);
        return request;
    }

    private UserAccount user(Long id, String nome, String email) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(email);
        user.setRole(UserRole.PARTICIPANTE);
        user.setAtivo(true);
        return user;
    }
}
