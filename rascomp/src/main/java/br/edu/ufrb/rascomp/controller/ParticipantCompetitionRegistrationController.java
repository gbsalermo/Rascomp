package br.edu.ufrb.rascomp.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationRequest;
import br.edu.ufrb.rascomp.service.ParticipantCompetitionRegistrationService;
import br.edu.ufrb.rascomp.service.RegistrationReceiptStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/participante/inscricoes-pessoais")
@RequiredArgsConstructor
public class ParticipantCompetitionRegistrationController {

    private final ParticipantCompetitionRegistrationService service;

    @GetMapping
    public ResponseEntity<List<ParticipantCompetitionRegistrationDTO>> minhas() {
        return ResponseEntity.ok(service.minhas());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParticipantCompetitionRegistrationDTO> criar(
            @Valid @RequestPart("dados") ParticipantCompetitionRegistrationRequest request,
            @RequestPart("comprovante") MultipartFile comprovante) {
        return ResponseEntity.status(201)
                .body(service.criarParaParticipanteAtual(request, comprovante));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelarMinha(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<Resource> comprovante(@PathVariable Long id) {
        return arquivo(service.comprovanteDoParticipante(id));
    }

    private ResponseEntity<Resource> arquivo(RegistrationReceiptStorageService.ReceiptFile file) {
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
