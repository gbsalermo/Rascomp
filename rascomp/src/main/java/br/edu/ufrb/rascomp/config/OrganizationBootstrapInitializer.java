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
public class OrganizationBootstrapInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final UserAccountService userAccountService;

    @Value("${rascomp.bootstrap.organizacao.nome:}")
    private String nome;

    @Value("${rascomp.bootstrap.organizacao.email:}")
    private String email;

    @Value("${rascomp.bootstrap.organizacao.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (userAccountRepository.countByRole(UserRole.ORGANIZACAO) > 0) return;
        if (nome == null || nome.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) return;

        RegisterRequest request = new RegisterRequest();
        request.setNome(nome);
        request.setEmail(email);
        request.setSenha(password);
        userAccountService.criarOrganizacao(request);

        System.out.println("Usuário inicial da ORGANIZAÇÃO criado a partir das variáveis de ambiente.");
    }
}
