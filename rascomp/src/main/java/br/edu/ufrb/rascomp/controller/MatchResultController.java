package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.service.MatchResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resultados-partida")
@RequiredArgsConstructor
@Tag(name = "Resultados de Partida")
public class MatchResultController {
    private final MatchResultService resultService;

    @GetMapping
    @Operation(summary = "Listar resultados de partida")
    public ResponseEntity<List<MatchResultDTO>> listarTodos() {
        return ResponseEntity.ok(resultService.listarTodos());
    }

    @GetMapping("/por-chaveamento")
    @Operation(summary = "Listar resultados por chaveamento")
    public ResponseEntity<List<MatchResultDTO>> listarPorChaveamento(
            @RequestParam Long bracketId) {
        return ResponseEntity.ok(
                resultService.listarPorChaveamento(bracketId));
    }

    @GetMapping("/por-competicao")
    @Operation(summary = "Listar resultados por competição")
    public ResponseEntity<List<MatchResultDTO>> listarPorCompeticao(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(
                resultService.listarPorCompeticao(competitionId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar resultado por ID")
    public ResponseEntity<MatchResultDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(resultService.buscarPorId(id));
    }

    @GetMapping("/por-partida")
    @Operation(summary = "Buscar resultado por partida")
    public ResponseEntity<MatchResultDTO> buscarPorPartida(@RequestParam Long matchId) {
        return ResponseEntity.ok(resultService.buscarPorPartida(matchId));
    }
}
