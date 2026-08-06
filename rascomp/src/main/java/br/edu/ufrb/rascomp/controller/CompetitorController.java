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

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.service.CompetitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/competidores")
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorService competitorService;

    @PostMapping
    public ResponseEntity<CompetitorDTO> criar(
            @Valid @RequestBody CompetitorDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(competitorService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompetitorDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {

        return ResponseEntity.ok(
                apenasAtivos
                        ? competitorService.listarAtivos()
                        : competitorService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                competitorService.buscarPorId(id)
        );
    }

    @GetMapping("/por-email")
    public ResponseEntity<CompetitorDTO> buscarPorEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(
                competitorService.buscarPorEmail(email)
        );
    }

    @GetMapping("/por-equipe")
    public ResponseEntity<List<CompetitorDTO>> listarPorEquipe(
            @RequestParam Long teamId,
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {

        return ResponseEntity.ok(
                competitorService.listarPorEquipe(teamId, apenasAtivos)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitorDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CompetitorDTO dto) {

        return ResponseEntity.ok(
                competitorService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        competitorService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<CompetitorDTO> reativar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                competitorService.reativar(id)
        );
    }
}