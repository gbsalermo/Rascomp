package br.edu.ufrb.rascomp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.AccountActionResponse;
import br.edu.ufrb.rascomp.dto.AuthResponse;
import br.edu.ufrb.rascomp.dto.EmailActionRequest;
import br.edu.ufrb.rascomp.dto.LoginRequest;
import br.edu.ufrb.rascomp.dto.PasswordResetRequest;
import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.RegisterResponse;
import br.edu.ufrb.rascomp.dto.TokenVerificationRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.service.AccountIdentityService;
import br.edu.ufrb.rascomp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountIdentityService accountIdentityService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.cadastrarParticipante(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<AccountActionResponse> resendVerification(
            @Valid @RequestBody EmailActionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AccountActionResponse(
                        accountIdentityService.resendVerification(request.getEmail())));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<AccountActionResponse> confirmEmail(
            @Valid @RequestBody TokenVerificationRequest request) {
        return ResponseEntity.ok(new AccountActionResponse(
                accountIdentityService.confirmEmail(request.getToken())));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<AccountActionResponse> forgotPassword(
            @Valid @RequestBody EmailActionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AccountActionResponse(
                        accountIdentityService.requestPasswordReset(request.getEmail())));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<AccountActionResponse> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(new AccountActionResponse(
                accountIdentityService.resetPassword(
                        request.getToken(),
                        request.getNovaSenha())));
    }

    @GetMapping("/me")
    public ResponseEntity<UserAccountDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
