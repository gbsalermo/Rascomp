package br.edu.ufrb.rascomp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.MatchJudgeDecisionDTO;
import br.edu.ufrb.rascomp.service.MatchJudgeDecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/decisoes-juiz-sumo")
@RequiredArgsConstructor
public class MatchJudgeDecisionController {

    private final MatchJudgeDecisionService decisionService;

    @PostMapping
    public ResponseEntity<MatchJudgeDecisionDTO> decidir(@Valid @RequestBody MatchJudgeDecisionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.decidir(dto));
    }

    @GetMapping("/por-partida")
    public ResponseEntity<MatchJudgeDecisionDTO> buscarPorPartida(@RequestParam Long matchId) {
        return ResponseEntity.ok(decisionService.buscarPorPartida(matchId));
    }
}
