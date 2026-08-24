package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class AccessPolicyServiceTest {

    @Mock private UserAccountService userAccountService;
    @Mock private TeamRepository teamRepository;
    @Mock private CompetitorRepository competitorRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    private AccessPolicyService service;

    @Test
    void responsavelPodeAcessarSuaEquipe() {
        UserAccount user = usuario(5L);
        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(user);

        when(userAccountService.buscarAtual()).thenReturn(user);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));

        assertEquals(9L, service.exigirEquipeDoResponsavel(9L).getId());
    }

    @Test
    void participanteNaoPodeAcessarEquipeDeOutroResponsavel() {
        UserAccount atual = usuario(5L);
        UserAccount outro = usuario(6L);
        Team team = new Team();
        team.setId(9L);
        team.setResponsibleUser(outro);

        when(userAccountService.buscarAtual()).thenReturn(atual);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));

        assertThrows(AccessDeniedException.class, () -> service.exigirEquipeDoResponsavel(9L));
    }

    private UserAccount usuario(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setAtivo(true);
        return user;
    }
}
