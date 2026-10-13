package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import br.edu.ufrb.rascomp.dto.AuthResponse;
import br.edu.ufrb.rascomp.dto.LoginRequest;
import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.RegisterResponse;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AccountIdentityService accountIdentityService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginDeveNormalizarEmailRegistrarAcessoERespeitarLembrarDeMim() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  USUARIO@EXEMPLO.COM ");
        request.setSenha("Rascomp@2026");
        request.setLembrarDeMim(true);

        UserAccount usuario = usuarioVerificado();
        when(userAccountService.buscarPorEmail("usuario@exemplo.com")).thenReturn(usuario);
        when(userAccountService.iniciarNovaSessao(usuario)).thenReturn(usuario);
        when(jwtService.gerarToken(usuario, true)).thenReturn("token-longo");

        AuthResponse response = authService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        assertEquals("usuario@exemplo.com", captor.getValue().getPrincipal());
        assertEquals("Rascomp@2026", captor.getValue().getCredentials());
        verify(userAccountService).iniciarNovaSessao(usuario);
        verify(jwtService).gerarToken(usuario, true);
        assertEquals("token-longo", response.getToken());
        assertEquals("usuario@exemplo.com", response.getUsuario().getEmail());
    }

    @Test
    void loginDeveBloquearContaSemEmailVerificadoMesmoComSenhaCorreta() {
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@exemplo.com");
        request.setSenha("Rascomp@2026");

        UserAccount usuario = usuarioVerificado();
        usuario.setEmailVerificadoEm(null);
        when(userAccountService.buscarPorEmail("usuario@exemplo.com")).thenReturn(usuario);

        assertThrows(AccessDeniedException.class, () -> authService.login(request));

        verify(userAccountService, never()).iniciarNovaSessao(usuario);
        verify(jwtService, never()).gerarToken(usuario, false);
    }

    @Test
    void cadastroDeveCriarContaPendenteEEnviarVerificacaoSemGerarJwt() {
        RegisterRequest request = new RegisterRequest();
        request.setNome("Participante");
        request.setEmail("participante@exemplo.com");
        request.setSenha("Rascomp@2026");

        UserAccount usuario = usuarioVerificado();
        usuario.setEmail("participante@exemplo.com");
        usuario.setEmailVerificadoEm(null);
        when(userAccountService.cadastrarParticipante(request)).thenReturn(usuario);

        RegisterResponse response = authService.cadastrarParticipante(request);

        verify(userAccountService).cadastrarParticipante(request);
        verify(accountIdentityService).sendInitialVerification(usuario);
        verify(userAccountService, never()).iniciarNovaSessao(usuario);
        verify(jwtService, never()).gerarToken(usuario, false);
        assertEquals("participante@exemplo.com", response.email());
    }

    @Test
    void logoutDeveInvalidarSessaoAtual() {
        authService.logout();
        verify(userAccountService).encerrarSessaoAtual();
    }

    private UserAccount usuarioVerificado() {
        UserAccount usuario = new UserAccount();
        usuario.setId(1L);
        usuario.setNome("Usuário");
        usuario.setEmail("usuario@exemplo.com");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setEmailVerificadoEm(LocalDateTime.now());
        return usuario;
    }
}
