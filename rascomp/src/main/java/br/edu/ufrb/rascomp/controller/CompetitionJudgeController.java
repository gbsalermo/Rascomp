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

import br.edu.ufrb.rascomp.dto.CompetitionJudgeDTO;
import br.edu.ufrb.rascomp.service.CompetitionJudgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/juizes-competicao")
@RequiredArgsConstructor
public class CompetitionJudgeController {

    private final CompetitionJudgeService judgeService;

    @PostMapping
    public ResponseEntity<CompetitionJudgeDTO> criar(@Valid @RequestBody CompetitionJudgeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(judgeService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompetitionJudgeDTO>> listar(
            @RequestParam Long competitionId,
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(judgeService.listar(competitionId, apenasAtivos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionJudgeDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionJudgeDTO dto) {
        return ResponseEntity.ok(judgeService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        judgeService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
