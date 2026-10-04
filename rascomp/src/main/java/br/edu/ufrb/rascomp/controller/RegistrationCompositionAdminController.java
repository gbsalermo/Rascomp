package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.RegistrationCompetitorChangeDTO;
import br.edu.ufrb.rascomp.dto.RegistrationCompetitorChangeReviewRequest;
import br.edu.ufrb.rascomp.service.RegistrationCompositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inscricoes/composicao")
@RequiredArgsConstructor
public class RegistrationCompositionAdminController {

    private final RegistrationCompositionService service;

    @GetMapping("/pendentes")
    public ResponseEntity<List<RegistrationCompetitorChangeDTO>> pendentes(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(service.listarPendentes(competitionId));
    }

    @GetMapping("/por-inscricao/{registrationId}")
    public ResponseEntity<List<RegistrationCompetitorChangeDTO>> porInscricao(
            @PathVariable Long registrationId) {
        return ResponseEntity.ok(service.listarPorInscricao(registrationId));
    }

    @PatchMapping("/{changeId}")
    public ResponseEntity<RegistrationCompetitorChangeDTO> revisar(
            @PathVariable Long changeId,
            @Valid @RequestBody RegistrationCompetitorChangeReviewRequest request) {
        return ResponseEntity.ok(service.revisarMudanca(changeId, request));
    }
}
