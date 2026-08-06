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

import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/partidas")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchDTO> criar(@Valid @RequestBody MatchDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.criar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.buscarPorId(id));
    }

    @GetMapping("/por-chaveamento")
    public ResponseEntity<List<MatchDTO>> listarPorChaveamento(@RequestParam Long bracketId) {
        return ResponseEntity.ok(matchService.listarPorChaveamento(bracketId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchDTO> atualizar(@PathVariable Long id, @Valid @RequestBody MatchDTO dto) {
        return ResponseEntity.ok(matchService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        matchService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<MatchDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.reativar(id));
    }
}
