package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.AccountTokenType;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountIdentityServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AccountTokenService accountTokenService;

    @Mock
    private TransactionalEmailService transactionalEmailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountIdentityService service;

    @BeforeEach
    void setUp() {
        service = new AccountIdentityService(
                userAccountRepository,
                accountTokenService,
                transactionalEmailService,
                passwordEncoder);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "verificationHours", 24L);
        ReflectionTestUtils.setField(service, "resetMinutes", 30L);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60L);
    }

    @Test
    void recuperacaoDeEmailInexistenteDeveResponderGenericoSemEmitirToken() {
        when(userAccountRepository.findByEmailIgnoreCase("ausente@exemplo.com"))
                .thenReturn(Optional.empty());

        String response = service.requestPasswordReset(" AUSENTE@EXEMPLO.COM ");

        assertEquals(
                "Se existir uma conta elegível para esse e-mail, enviaremos as instruções de recuperação.",
                response);
        verify(accountTokenService, never()).issue(any(), any(), any());
    }

    @Test
    void resetDeSenhaDeveTrocarHashEInvalidarSessoesAnteriores() {
        UserAccount user = user();
        user.setSessionVersion(7L);
        when(accountTokenService.consume("reset-token", AccountTokenType.PASSWORD_RESET))
                .thenReturn(user);
        when(passwordEncoder.encode("NovaSenha123")).thenReturn("novo-hash");
        when(userAccountRepository.save(user)).thenReturn(user);

        service.resetPassword("reset-token", "NovaSenha123");

        assertEquals("novo-hash", user.getPasswordHash());
        assertEquals(8L, user.getSessionVersion());
        verify(userAccountRepository).save(user);
    }

    @Test
    void confirmacaoDeEmailDeveMarcarVerificadoEInvalidarSessaoAnterior() {
        UserAccount user = user();
        user.setEmailVerificadoEm(null);
        user.setSessionVersion(3L);
        when(accountTokenService.consume("verify-token", AccountTokenType.EMAIL_VERIFICATION))
                .thenReturn(user);
        when(userAccountRepository.save(user)).thenReturn(user);

        service.confirmEmail("verify-token");

        assertEquals(4L, user.getSessionVersion());
        verify(userAccountRepository).save(user);
    }

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setNome("Participante");
        user.setEmail("participante@exemplo.com");
        user.setAtivo(true);
        user.setEmailVerificadoEm(LocalDateTime.now());
        user.setPasswordHash("hash-antigo");
        return user;
    }
}
