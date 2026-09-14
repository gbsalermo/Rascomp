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
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new UserAccountService(userAccountRepository, passwordEncoder);
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
