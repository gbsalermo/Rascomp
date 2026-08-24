package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZACAO')")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping("/organizacao")
    public ResponseEntity<UserAccountDTO> criarOrganizacao(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userAccountService.criarOrganizacao(request));
    }

    @GetMapping
    public ResponseEntity<List<UserAccountDTO>> listar(@RequestParam UserRole role) {
        return ResponseEntity.ok(userAccountService.listarPorRole(role));
    }

    @PatchMapping("/{id}/ativo")
    public ResponseEntity<UserAccountDTO> alterarAtivo(
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        return ResponseEntity.ok(userAccountService.alterarAtivo(id, ativo));
    }
}
