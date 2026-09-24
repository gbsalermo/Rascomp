package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountUpdateRequest;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CompetitorRepository competitorRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new UserAccountService(userAccountRepository, competitorRepository, passwordEncoder);
    }

    @Test
    void cadastrarParticipanteDeveSalvarSomenteHashBCrypt() {
        RegisterRequest request = request("Participante", "Teste@Email.com", "SenhaForte123");

        when(userAccountRepository.existsByEmailIgnoreCase("teste@email.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        UserAccount salvo = service.cadastrarParticipante(request);

        assertEquals(10L, salvo.getId());
        assertEquals("teste@email.com", salvo.getEmail());
        assertEquals(UserRole.PARTICIPANTE, salvo.getRole());
        assertNotEquals(request.getSenha(), salvo.getPasswordHash());
        assertTrue(passwordEncoder.matches(request.getSenha(), salvo.getPasswordHash()));
    }

    @Test
    void cadastrarParticipanteDeveRejeitarEmailDuplicado() {
        RegisterRequest request = request("Participante", "teste@email.com", "SenhaForte123");
        when(userAccountRepository.existsByEmailIgnoreCase("teste@email.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrarParticipante(request));

        assertTrue(ex.getMessage().contains("e-mail"));
    }

    @Test
    void criarInternoDeveCriarGestaoComRoleExplicita() {
        RegisterRequest request = request("Gestão", "gestao@rascomp.com", "OutraSenha123");

        when(userAccountRepository.existsByEmailIgnoreCase("gestao@rascomp.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount entity = invocation.getArgument(0);
            entity.setId(19L);
            return entity;
        });

        var dto = service.criarInterno(request, UserRole.GESTAO);

        assertEquals(UserRole.GESTAO, dto.getRole());
    }

    @Test
    void criarInternoDeveRejeitarParticipante() {
        RegisterRequest request = request("Participante", "participante.interno@rascomp.com", "OutraSenha123");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarInterno(request, UserRole.PARTICIPANTE));

        assertTrue(ex.getMessage().contains("cadastro comum"));
    }

    @Test
    void alterarRoleInternaDevePermitirGestaoParaMidia() {
        UserAccount usuario = new UserAccount();
        usuario.setId(30L);
        usuario.setEmail("gestao@rascomp.com");
        usuario.setRole(UserRole.GESTAO);
        usuario.setAtivo(true);

        when(userAccountRepository.findById(30L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.alterarRoleInterna(30L, UserRole.MIDIA);

        assertEquals(UserRole.MIDIA, dto.getRole());
        verify(userAccountRepository).save(usuario);
    }

    @Test
    void alterarRoleInternaDeveRejeitarConversaoDeParticipante() {
        UserAccount usuario = new UserAccount();
        usuario.setId(31L);
        usuario.setEmail("participante@rascomp.com");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);

        when(userAccountRepository.findById(31L)).thenReturn(java.util.Optional.of(usuario));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarRoleInterna(31L, UserRole.GESTAO));

        assertTrue(ex.getMessage().contains("identidade separada"));
    }

    @Test
    void alterarRoleInternaDeveRejeitarRoleParticipante() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarRoleInterna(32L, UserRole.PARTICIPANTE));

        assertTrue(ex.getMessage().contains("DEV, GESTAO ou MIDIA"));
    }

    @Test
    void alterarRoleInternaDeveProtegerUltimoDevAtivo() {
        UserAccount usuario = new UserAccount();
        usuario.setId(33L);
        usuario.setEmail("outro.dev@rascomp.com");
        usuario.setRole(UserRole.DEV);
        usuario.setAtivo(true);

        when(userAccountRepository.findById(33L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.countByRoleAndAtivoTrue(UserRole.DEV)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarRoleInterna(33L, UserRole.GESTAO));

        assertTrue(ex.getMessage().contains("pelo menos um DEV ativo"));
    }

    @Test
    void alterarAtivoDeveImpedirDesativarContaAtual() {
        UserAccount usuario = new UserAccount();
        usuario.setId(35L);
        usuario.setEmail("atual@rascomp.com");
        usuario.setRole(UserRole.DEV);
        usuario.setAtivo(true);

        when(userAccountRepository.findById(35L)).thenReturn(java.util.Optional.of(usuario));

        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "atual@rascomp.com", null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.alterarAtivo(35L, false));
            assertTrue(ex.getMessage().contains("atualmente autenticada"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void alterarAtivoDeveProtegerUltimoDevAtivo() {
        UserAccount usuario = new UserAccount();
        usuario.setId(34L);
        usuario.setEmail("ultimo.dev@rascomp.com");
        usuario.setRole(UserRole.DEV);
        usuario.setAtivo(true);

        when(userAccountRepository.findById(34L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.countByRoleAndAtivoTrue(UserRole.DEV)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarAtivo(34L, false));

        assertTrue(ex.getMessage().contains("pelo menos um DEV ativo"));
    }

    @Test
    void atualizarDadosDeveEditarParticipanteSemAlterarRole() {
        UserAccount usuario = new UserAccount();
        usuario.setId(41L);
        usuario.setNome("Nome antigo");
        usuario.setEmail("antigo@rascomp.com");
        usuario.setTelefone("111");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setSessionVersion(2L);

        UserAccountUpdateRequest request = new UserAccountUpdateRequest();
        request.setNome(" Participante Atualizado ");
        request.setEmail("NOVO@RASCOMP.COM");
        request.setTelefone(" 75999990000 ");

        when(userAccountRepository.findById(41L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.existsByEmailIgnoreCaseAndIdNot("novo@rascomp.com", 41L)).thenReturn(false);
        when(userAccountRepository.save(usuario)).thenReturn(usuario);

        var dto = service.atualizarDados(41L, request);

        assertEquals("Participante Atualizado", dto.getNome());
        assertEquals("novo@rascomp.com", dto.getEmail());
        assertEquals("75999990000", dto.getTelefone());
        assertEquals(UserRole.PARTICIPANTE, dto.getRole());
        assertEquals(3L, usuario.getSessionVersion());
    }

    @Test
    void atualizarDadosDeveRejeitarEmailJaUsadoPorOutraConta() {
        UserAccount usuario = new UserAccount();
        usuario.setId(42L);
        usuario.setEmail("atual@rascomp.com");
        usuario.setRole(UserRole.GESTAO);

        UserAccountUpdateRequest request = new UserAccountUpdateRequest();
        request.setNome("Gestão");
        request.setEmail("duplicado@rascomp.com");

        when(userAccountRepository.findById(42L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.existsByEmailIgnoreCaseAndIdNot("duplicado@rascomp.com", 42L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.atualizarDados(42L, request));
    }

    @Test
    void alterarAtivoParticipanteDeveSincronizarCompetidorESinalizarEquipeSemAtivos() {
        UserAccount usuario = new UserAccount();
        usuario.setId(50L);
        usuario.setNome("Participante");
        usuario.setEmail("participante@rascomp.com");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setSessionVersion(1L);

        Institution institution = new Institution();
        institution.setId(1L);
        institution.setNome("UFRB");
        institution.setAtivo(true);

        Team team = new Team();
        team.setId(2L);
        team.setNome("Equipe Única");
        team.setInstitution(institution);
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(3L);
        competitor.setNome("Participante");
        competitor.setEmail("participante@rascomp.com");
        competitor.setTeam(team);
        competitor.setUserAccount(usuario);
        competitor.setAtivo(true);

        when(userAccountRepository.findById(50L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.save(usuario)).thenReturn(usuario);
        when(competitorRepository.findByUserAccountId(50L)).thenReturn(java.util.Optional.of(competitor));
        when(competitorRepository.save(competitor)).thenReturn(competitor);
        when(competitorRepository.countByTeamIdAndAtivoTrue(2L)).thenReturn(0L);

        var dto = service.alterarAtivo(50L, false);

        assertEquals(false, usuario.getAtivo());
        assertEquals(false, competitor.getAtivo());
        assertEquals(true, dto.getTeamWithoutActiveCompetitors());
        assertEquals("Equipe Única", dto.getCompetitorTeamNome());
        verify(competitorRepository).save(competitor);
    }

    @Test
    void atualizarDadosParticipanteDeveSincronizarCompetidorVinculado() {
        UserAccount usuario = new UserAccount();
        usuario.setId(51L);
        usuario.setNome("Nome antigo");
        usuario.setEmail("antigo@rascomp.com");
        usuario.setTelefone("111");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setSessionVersion(1L);

        Team team = new Team();
        team.setId(5L);
        team.setNome("Equipe");
        team.setAtivo(true);

        Competitor competitor = new Competitor();
        competitor.setId(6L);
        competitor.setNome("Nome antigo");
        competitor.setEmail("antigo@rascomp.com");
        competitor.setTelefone("111");
        competitor.setTeam(team);
        competitor.setUserAccount(usuario);
        competitor.setAtivo(true);

        UserAccountUpdateRequest request = new UserAccountUpdateRequest();
        request.setNome("Nome novo");
        request.setEmail("novo@rascomp.com");
        request.setTelefone("222");

        when(userAccountRepository.findById(51L)).thenReturn(java.util.Optional.of(usuario));
        when(userAccountRepository.existsByEmailIgnoreCaseAndIdNot("novo@rascomp.com", 51L)).thenReturn(false);
        when(competitorRepository.findByUserAccountId(51L)).thenReturn(java.util.Optional.of(competitor));
        when(competitorRepository.existsByEmailIgnoreCaseAndIdNot("novo@rascomp.com", 6L)).thenReturn(false);
        when(userAccountRepository.save(usuario)).thenReturn(usuario);
        when(competitorRepository.save(competitor)).thenReturn(competitor);
        when(competitorRepository.countByTeamIdAndAtivoTrue(5L)).thenReturn(1L);

        var dto = service.atualizarDados(51L, request);

        assertEquals("Nome novo", competitor.getNome());
        assertEquals("novo@rascomp.com", competitor.getEmail());
        assertEquals("222", competitor.getTelefone());
        assertEquals(6L, dto.getCompetitorId());
        verify(competitorRepository).save(competitor);
    }

    @Test
    void iniciarNovaSessaoDeveIncrementarVersaoERegistrarLogin() {
        UserAccount usuario = new UserAccount();
        usuario.setId(40L);
        usuario.setEmail("sessao@rascomp.com");
        usuario.setRole(UserRole.GESTAO);
        usuario.setAtivo(true);
        usuario.setSessionVersion(7L);

        when(userAccountRepository.save(usuario)).thenReturn(usuario);

        UserAccount atualizado = service.iniciarNovaSessao(usuario);

        assertEquals(8L, atualizado.getSessionVersion());
        assertTrue(atualizado.getUltimoLogin() != null);
        verify(userAccountRepository).save(usuario);
    }

    @Test
    void criarDevDeveUsarRoleDevEHash() {
        RegisterRequest request = request("Organização", "org@rascomp.com", "OutraSenha123");

        when(userAccountRepository.existsByEmailIgnoreCase("org@rascomp.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });

        var dto = service.criarDev(request);

        assertEquals(UserRole.DEV, dto.getRole());
    }

    private RegisterRequest request(String nome, String email, String senha) {
        RegisterRequest request = new RegisterRequest();
        request.setNome(nome);
        request.setEmail(email);
        request.setSenha(senha);
        request.setTelefone(" 75999999999 ");
        return request;
    }
}
