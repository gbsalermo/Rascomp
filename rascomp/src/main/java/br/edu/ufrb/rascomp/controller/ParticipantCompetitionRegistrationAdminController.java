package br.edu.ufrb.rascomp.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.ManualParticipantEntryRequest;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationReviewRequest;
import br.edu.ufrb.rascomp.service.ManualCompetitionEntryService;
import br.edu.ufrb.rascomp.service.ParticipantCompetitionRegistrationService;
import br.edu.ufrb.rascomp.service.RegistrationReceiptStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inscricoes-participantes")
@RequiredArgsConstructor
public class ParticipantCompetitionRegistrationAdminController {

    private final ParticipantCompetitionRegistrationService service;
    private final ManualCompetitionEntryService manualCompetitionEntryService;

    @org.springframework.web.bind.annotation.PostMapping("/entrada-manual")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('DEV')")
    public ResponseEntity<ParticipantCompetitionRegistrationDTO> entradaManual(
            @Valid @RequestBody ManualParticipantEntryRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(manualCompetitionEntryService.criarParticipante(request));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<ParticipantCompetitionRegistrationDTO>> listar(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(service.listarPorCompeticao(competitionId));
    }

    @PatchMapping("/{id}/revisao")
    public ResponseEntity<ParticipantCompetitionRegistrationDTO> revisar(
            @PathVariable Long id,
            @Valid @RequestBody ParticipantCompetitionRegistrationReviewRequest request) {
        return ResponseEntity.ok(service.revisar(id, request));
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<Resource> comprovante(@PathVariable Long id) {
        RegistrationReceiptStorageService.ReceiptFile file =
                service.comprovanteAdministrativo(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(file.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(file.resource());
    }
}
