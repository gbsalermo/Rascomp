package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.service.BracketService;
import br.edu.ufrb.rascomp.service.CompetitionContextService;
import br.edu.ufrb.rascomp.service.MatchResultService;
import br.edu.ufrb.rascomp.service.MatchService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resultados-partida")
@RequiredArgsConstructor
public class MatchResultController {
    private final MatchResultService resultService;
    private final BracketService bracketService;
    private final MatchService matchService;
    private final CompetitionContextService competitionContextService;

    @GetMapping
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<List<MatchResultDTO>> listarTodos() {
        return ResponseEntity.ok(resultService.listarTodos());
    }

    @GetMapping("/por-chaveamento")
    public ResponseEntity<List<MatchResultDTO>> listarPorChaveamento(
            @RequestParam Long bracketId) {
        var bracket = bracketService.buscarPorId(bracketId);
        competitionContextService.exigirOperavel(bracket.getCompetitionId());
        return ResponseEntity.ok(
                resultService.listarPorChaveamento(bracketId));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<MatchResultDTO>> listarPorCompeticao(
            @RequestParam Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        return ResponseEntity.ok(
                resultService.listarPorCompeticao(competitionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResultDTO> buscarPorId(@PathVariable Long id) {
        MatchResultDTO result = resultService.buscarPorId(id);
        var match = matchService.buscarPorId(result.getMatchId());
        competitionContextService.exigirOperavel(match.getCompetitionId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/por-partida")
    public ResponseEntity<MatchResultDTO> buscarPorPartida(@RequestParam Long matchId) {
        var match = matchService.buscarPorId(matchId);
        competitionContextService.exigirOperavel(match.getCompetitionId());
        return ResponseEntity.ok(resultService.buscarPorPartida(matchId));
    }
}
