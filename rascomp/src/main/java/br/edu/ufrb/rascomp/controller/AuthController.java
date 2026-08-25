package br.edu.ufrb.rascomp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.AuthResponse;
import br.edu.ufrb.rascomp.dto.LoginRequest;
import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Cadastrar participante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta PARTICIPANTE criada e JWT retornado."),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou e-mail já cadastrado.")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.cadastrarParticipante(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciais válidas; JWT retornado."),
            @ApiResponse(responseCode = "401", description = "E-mail/senha inválidos ou conta desativada.")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário identificado pelo JWT atual."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.")
    })
    public ResponseEntity<UserAccountDTO> me() {
        return ResponseEntity.ok(authService.me());
    }
}
