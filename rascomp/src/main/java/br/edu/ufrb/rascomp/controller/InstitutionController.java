package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.InstitutionDTO;
import br.edu.ufrb.rascomp.service.InstitutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/instituicoes")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @PostMapping
    public ResponseEntity<InstitutionDTO> criar(
            @Valid @RequestBody InstitutionDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(institutionService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<InstitutionDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivas) {

        if (apenasAtivas) {
            return ResponseEntity.ok(
                    institutionService.listarAtivas()
            );
        }

        return ResponseEntity.ok(
                institutionService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstitutionDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                institutionService.buscarPorId(id)
        );
    }

    @GetMapping("/por-sigla")
    public ResponseEntity<InstitutionDTO> buscarPorSigla(
            @RequestParam String sigla) {

        return ResponseEntity.ok(
                institutionService.buscarPorSigla(sigla)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<InstitutionDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionDTO dto) {

        return ResponseEntity.ok(
                institutionService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        institutionService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<InstitutionDTO> reativar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                institutionService.reativar(id)
        );
    }
}