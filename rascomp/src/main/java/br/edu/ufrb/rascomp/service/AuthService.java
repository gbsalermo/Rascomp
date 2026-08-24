package br.edu.ufrb.rascomp.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.AuthResponse;
import br.edu.ufrb.rascomp.dto.LoginRequest;
import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse cadastrarParticipante(RegisterRequest request) {
        UserAccount usuario = userAccountService.cadastrarParticipante(request);
        return new AuthResponse(jwtService.gerarToken(usuario), usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getSenha()));

        UserAccount usuario = userAccountService.buscarPorEmail(email);
        userAccountService.registrarLogin(usuario);
        return new AuthResponse(jwtService.gerarToken(usuario), usuario);
    }

    @Transactional(readOnly = true)
    public UserAccountDTO me() {
        return new UserAccountDTO(userAccountService.buscarAtual());
    }
}
