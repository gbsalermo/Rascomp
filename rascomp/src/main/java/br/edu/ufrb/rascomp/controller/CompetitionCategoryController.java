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

import br.edu.ufrb.rascomp.dto.CompetitionCategoryDTO;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.service.CompetitionCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CompetitionCategoryController {

    private final CompetitionCategoryService competitionCategoryService;

    @PostMapping
    public ResponseEntity<CompetitionCategoryDTO> criar(
            @Valid @RequestBody CompetitionCategoryDTO dto) {

        CompetitionCategoryDTO novaCategoria =
                competitionCategoryService.criar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(novaCategoria);
    }

    @GetMapping
    public ResponseEntity<List<CompetitionCategoryDTO>> listarTodos() {
        return ResponseEntity.ok(
                competitionCategoryService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionCategoryDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                competitionCategoryService.buscarPorId(id));
    }

    @GetMapping("/por-modalidade")
    public ResponseEntity<List<CompetitionCategoryDTO>> listarPorModalidade(
            @RequestParam Modalidade modalidade) {

        return ResponseEntity.ok(
                competitionCategoryService.listarPorModalidade(modalidade));
    }

    @GetMapping("/por-modalidade/ativas")
    public ResponseEntity<List<CompetitionCategoryDTO>> listarPorModalidadeAtiva(
            @RequestParam Modalidade modalidade) {

        return ResponseEntity.ok(
                competitionCategoryService.listarPorModalidadeAtiva(modalidade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionCategoryDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionCategoryDTO dto) {

        return ResponseEntity.ok(
                competitionCategoryService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        competitionCategoryService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}