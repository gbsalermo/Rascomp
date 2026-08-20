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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resultados-partida")
@RequiredArgsConstructor
public class MatchResultController {
    private final MatchResultService resultService;

    @GetMapping
    public ResponseEntity<List<MatchResultDTO>> listarTodos() {
        return ResponseEntity.ok(resultService.listarTodos());
    }

    @GetMapping("/por-chaveamento")
    public ResponseEntity<List<MatchResultDTO>> listarPorChaveamento(
            @RequestParam Long bracketId) {
        return ResponseEntity.ok(
                resultService.listarPorChaveamento(bracketId));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<MatchResultDTO>> listarPorCompeticao(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(
                resultService.listarPorCompeticao(competitionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResultDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(resultService.buscarPorId(id));
    }

    @GetMapping("/por-partida")
    public ResponseEntity<MatchResultDTO> buscarPorPartida(@RequestParam Long matchId) {
        return ResponseEntity.ok(resultService.buscarPorPartida(matchId));
    }
}
