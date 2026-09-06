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

import br.edu.ufrb.rascomp.dto.RegistrationCancellationDecisionRequest;
import br.edu.ufrb.rascomp.dto.RegistrationCancellationRequestDTO;
import br.edu.ufrb.rascomp.model.Enum.StatusCancellationRequest;
import br.edu.ufrb.rascomp.service.RegistrationCancellationRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/solicitacoes-cancelamento-inscricao")
@RequiredArgsConstructor
public class RegistrationCancellationRequestController {

    private final RegistrationCancellationRequestService service;

    @GetMapping
    public ResponseEntity<List<RegistrationCancellationRequestDTO>> listar(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) StatusCancellationRequest status) {
        return ResponseEntity.ok(service.listar(competitionId, status));
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<RegistrationCancellationRequestDTO> aprovar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RegistrationCancellationDecisionRequest request) {
        return ResponseEntity.ok(service.aprovar(id, request == null ? null : request.getResposta()));
    }

    @PatchMapping("/{id}/rejeitar")
    public ResponseEntity<RegistrationCancellationRequestDTO> rejeitar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RegistrationCancellationDecisionRequest request) {
        return ResponseEntity.ok(service.rejeitar(id, request == null ? null : request.getResposta()));
    }
}
