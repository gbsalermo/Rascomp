package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.CompetitionCategoryResultDTO;
import br.edu.ufrb.rascomp.service.CompetitionResultsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resultados-competicao")
@RequiredArgsConstructor
public class CompetitionResultsController {

    private final CompetitionResultsService service;

    @GetMapping
    public ResponseEntity<List<CompetitionCategoryResultDTO>> listar(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(service.listar(competitionId));
    }
}
