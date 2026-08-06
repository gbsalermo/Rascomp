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

import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/competicoes")
@RequiredArgsConstructor
public class CompetitionController {
    private final CompetitionService competitionService;

    @PostMapping
    public ResponseEntity<CompetitionDTO> criar(@Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitionService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompetitionDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivas) {
        return ResponseEntity.ok(competitionService.listar(apenasAtivas));
    }

    @GetMapping("/por-status")
    public ResponseEntity<List<CompetitionDTO>> listarPorStatus(@RequestParam StatusCompetition status) {
        return ResponseEntity.ok(competitionService.listarPorStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.ok(competitionService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        competitionService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<CompetitionDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.reativar(id));
    }
}
