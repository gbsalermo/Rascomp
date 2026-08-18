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

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.service.BracketGenerationService;
import br.edu.ufrb.rascomp.service.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chaveamentos")
@RequiredArgsConstructor
public class BracketController {
    private final BracketService bracketService;
    private final BracketGenerationService bracketGenerationService;

    @PostMapping
    public ResponseEntity<BracketDTO> criar(@Valid @RequestBody BracketDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bracketService.criar(dto));
    }

    @PostMapping("/gerar")
    public ResponseEntity<BracketDTO> gerar(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bracketGenerationService.gerar(competitionId, categoryId));
    }

    @GetMapping
    public ResponseEntity<List<BracketDTO>> listar(@RequestParam(defaultValue = "false") boolean apenasAtivos) {
        return ResponseEntity.ok(bracketService.listar(apenasAtivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BracketDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bracketService.buscarPorId(id));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<BracketDTO>> listarPorCompeticao(@RequestParam Long competitionId) {
        return ResponseEntity.ok(bracketService.listarPorCompeticao(competitionId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BracketDTO> atualizar(@PathVariable Long id, @Valid @RequestBody BracketDTO dto) {
        return ResponseEntity.ok(bracketService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        bracketService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<BracketDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(bracketService.reativar(id));
    }
}
