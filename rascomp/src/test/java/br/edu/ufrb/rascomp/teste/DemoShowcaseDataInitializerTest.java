package br.edu.ufrb.rascomp.teste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;

class DemoShowcaseDataInitializerTest {

    private UserAccountRepository userAccountRepository;
    private PasswordEncoder passwordEncoder;
    private DemoShowcaseDataInitializer initializer;

    @BeforeEach
    void setup() {
        userAccountRepository = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        initializer = mock(DemoShowcaseDataInitializer.class, CALLS_REAL_METHODS);

        ReflectionTestUtils.setField(initializer, "userAccountRepository", userAccountRepository);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", passwordEncoder);

        when(userAccountRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(DemoShowcaseDataInitializer.DEMO_PASSWORD)).thenReturn("encoded-demo-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveCriarUsuariosDeDemonstracaoParaTodosOsPerfisDaEtapa3() {
        List<UserAccount> usuarios = initializer.garantirUsuariosDeAcesso();

        assertEquals(4, usuarios.size());

        assertUsuario(usuarios.get(0), DemoShowcaseDataInitializer.DEV_EMAIL, UserRole.DEV);
        assertUsuario(usuarios.get(1), DemoShowcaseDataInitializer.MANAGEMENT_EMAIL, UserRole.GESTAO);
        assertUsuario(usuarios.get(2), DemoShowcaseDataInitializer.MEDIA_EMAIL, UserRole.MIDIA);
        assertUsuario(usuarios.get(3), DemoShowcaseDataInitializer.PARTICIPANT_EMAIL, UserRole.PARTICIPANTE);

        verify(passwordEncoder, org.mockito.Mockito.times(4))
                .encode(DemoShowcaseDataInitializer.DEMO_PASSWORD);
        verify(userAccountRepository, org.mockito.Mockito.times(4)).save(any(UserAccount.class));
    }

    private void assertUsuario(UserAccount usuario, String email, UserRole role) {
        assertEquals(email, usuario.getEmail());
        assertEquals(role, usuario.getRole());
        assertEquals("encoded-demo-password", usuario.getPasswordHash());
        assertTrue(usuario.getAtivo());
    }
}
