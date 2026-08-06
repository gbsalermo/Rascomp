package br.edu.ufrb.rascomp.controller;

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

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.service.MatchResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resultados-partida")
@RequiredArgsConstructor
public class MatchResultController {
    private final MatchResultService resultService;

    @PostMapping
    public ResponseEntity<MatchResultDTO> criar(@Valid @RequestBody MatchResultDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resultService.criar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResultDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(resultService.buscarPorId(id));
    }

    @GetMapping("/por-partida")
    public ResponseEntity<MatchResultDTO> buscarPorPartida(@RequestParam Long matchId) {
        return ResponseEntity.ok(resultService.buscarPorPartida(matchId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchResultDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody MatchResultDTO dto) {
        return ResponseEntity.ok(resultService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        resultService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
