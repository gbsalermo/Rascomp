package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.AccountActionResponse;
import br.edu.ufrb.rascomp.dto.InternalAccountCreateRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.dto.UserAccountUpdateRequest;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEV')")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping("/dev")
    public ResponseEntity<UserAccountDTO> criarDev(@Valid @RequestBody InternalAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userAccountService.criarInterno(request, UserRole.DEV));
    }

    @PostMapping("/internos")
    public ResponseEntity<UserAccountDTO> criarInterno(
            @Valid @RequestBody InternalAccountCreateRequest request,
            @RequestParam UserRole role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userAccountService.criarInterno(request, role));
    }

    @PostMapping("/{id}/reenviar-convite")
    public ResponseEntity<AccountActionResponse> reenviarConvite(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AccountActionResponse(userAccountService.reenviarConviteInterno(id)));
    }

    @GetMapping
    public ResponseEntity<List<UserAccountDTO>> listar(@RequestParam UserRole role) {
        return ResponseEntity.ok(userAccountService.listarPorRole(role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountDTO> atualizarDados(
            @PathVariable Long id,
            @Valid @RequestBody UserAccountUpdateRequest request) {
        return ResponseEntity.ok(userAccountService.atualizarDados(id, request));
    }

    @PatchMapping("/{id}/ativo")
    public ResponseEntity<UserAccountDTO> alterarAtivo(
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        return ResponseEntity.ok(userAccountService.alterarAtivo(id, ativo));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserAccountDTO> alterarRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {
        return ResponseEntity.ok(userAccountService.alterarRoleInterna(id, role));
    }
}
