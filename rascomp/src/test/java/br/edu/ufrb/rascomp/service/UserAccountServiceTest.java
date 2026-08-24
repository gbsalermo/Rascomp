package br.edu.ufrb.rascomp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    void criarOrganizacaoDeveUsarRoleOrganizacaoEHash() {
        RegisterRequest request = request("Organização", "org@rascomp.com", "OutraSenha123");

        when(userAccountRepository.existsByEmailIgnoreCase("org@rascomp.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount entity = invocation.getArgument(0);
            entity.setId(20L);
            return entity;
        });

        var dto = service.criarOrganizacao(request);

        assertEquals(UserRole.ORGANIZACAO, dto.getRole());
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
