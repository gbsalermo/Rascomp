package br.edu.ufrb.rascomp.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.AuthResponse;
import br.edu.ufrb.rascomp.dto.LoginRequest;
import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.RegisterResponse;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final AccountIdentityService accountIdentityService;
    private final JwtService jwtService;

    public RegisterResponse cadastrarParticipante(RegisterRequest request) {
        UserAccount usuario = userAccountService.cadastrarParticipante(request);
        accountIdentityService.sendInitialVerification(usuario);
        return new RegisterResponse(
                "Conta criada. Confirme o e-mail antes de entrar.",
                usuario.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getSenha()));

        UserAccount usuario = userAccountService.buscarPorEmail(email);
        if (!usuario.isEmailVerified()) {
            throw new AccessDeniedException(
                    "Confirme seu e-mail antes de entrar. Se necessário, solicite um novo link de verificação.");
        }

        usuario = userAccountService.iniciarNovaSessao(usuario);
        return new AuthResponse(jwtService.gerarToken(usuario, request.isLembrarDeMim()), usuario);
    }

    @Transactional(readOnly = true)
    public UserAccountDTO me() {
        return new UserAccountDTO(userAccountService.buscarAtual());
    }

    @Transactional
    public void logout() {
        userAccountService.encerrarSessaoAtual();
    }
}
