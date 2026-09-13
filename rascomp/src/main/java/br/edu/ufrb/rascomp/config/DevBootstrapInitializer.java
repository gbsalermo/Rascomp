package br.edu.ufrb.rascomp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.service.UserAccountService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DevBootstrapInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final UserAccountService userAccountService;

    @Value("${rascomp.bootstrap.dev.nome:}")
    private String nome;

    @Value("${rascomp.bootstrap.dev.email:}")
    private String email;

    @Value("${rascomp.bootstrap.dev.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (userAccountRepository.countByRole(UserRole.DEV) > 0) return;
        if (nome == null || nome.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) return;

        RegisterRequest request = new RegisterRequest();
        request.setNome(nome);
        request.setEmail(email);
        request.setSenha(password);
        userAccountService.criarDev(request);

        System.out.println("Usuário inicial DEV criado a partir das variáveis de ambiente.");
    }
}
