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

import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TeamOwnershipServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @InjectMocks
    private TeamService service;

    @Test
    void criarParaResponsavelDeveAssociarParticipanteAutenticado() {
        Institution institution = instituicao();
        UserAccount participante = usuario(7L, UserRole.PARTICIPANTE, true);

        TeamDTO dto = new TeamDTO();
        dto.setNome("Equipe Nova");
        dto.setInstitutionId(1L);

        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(teamRepository.existsByNomeIgnoreCaseAndInstitutionId("Equipe Nova", 1L)).thenReturn(false);
        when(userAccountRepository.findById(7L)).thenReturn(Optional.of(participante));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(11L);
            return team;
        });

        TeamDTO resultado = service.criarParaResponsavel(dto, participante);

        assertEquals(7L, resultado.getResponsibleUserId());
        assertEquals("Responsável", resultado.getResponsibleUserNome());
    }

    @Test
    void responsavelDeEquipeNaoPodeSerUsuarioOrganizacao() {
        UserAccount organizacao = usuario(2L, UserRole.ORGANIZACAO, true);
        TeamDTO dto = new TeamDTO();
        dto.setNome("Equipe");
        dto.setInstitutionId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.criarParaResponsavel(dto, organizacao));
    }

    private Institution instituicao() {
        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNome("UFRB");
        institution.setSigla("UFRB");
        institution.setAtivo(true);
        return institution;
    }

    private UserAccount usuario(Long id, UserRole role, boolean ativo) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setNome("Responsável");
        user.setEmail("responsavel@teste.com");
        user.setRole(role);
        user.setAtivo(ativo);
        return user;
    }
}
