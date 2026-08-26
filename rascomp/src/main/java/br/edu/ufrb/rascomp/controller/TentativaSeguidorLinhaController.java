package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.service.TentativaSeguidorLinhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tentativas-seguidor-linha")
@RequiredArgsConstructor
public class TentativaSeguidorLinhaController {
    private final TentativaSeguidorLinhaService tentativaService;

    @PostMapping
    public ResponseEntity<TentativaSeguidorLinhaDTO> criar(@Valid @RequestBody TentativaSeguidorLinhaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tentativaService.criar(dto));
    }

    @GetMapping("/por-contexto")
    public ResponseEntity<List<TentativaSeguidorLinhaDTO>> listarPorContexto(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(tentativaService.listarPorContexto(competitionId, categoryId));
    }

    @GetMapping("/por-inscricao")
    public ResponseEntity<List<TentativaSeguidorLinhaDTO>> listarPorInscricao(@RequestParam Long registrationId) {
        return ResponseEntity.ok(tentativaService.listarPorInscricao(registrationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TentativaSeguidorLinhaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tentativaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TentativaSeguidorLinhaDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody TentativaSeguidorLinhaDTO dto) {
        return ResponseEntity.ok(tentativaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        tentativaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
