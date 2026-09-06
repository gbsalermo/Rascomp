package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.AusenciaTomadaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.service.AusenciaTomadaSeguidorLinhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ausencias-tomada-seguidor-linha")
@RequiredArgsConstructor
public class AusenciaTomadaSeguidorLinhaController {

    private final AusenciaTomadaSeguidorLinhaService ausenciaService;

    @PostMapping
    public ResponseEntity<AusenciaTomadaSeguidorLinhaDTO> marcar(
            @Valid @RequestBody AusenciaTomadaSeguidorLinhaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ausenciaService.marcar(dto));
    }

    @GetMapping("/por-inscricao")
    public ResponseEntity<List<AusenciaTomadaSeguidorLinhaDTO>> listarPorInscricao(
            @RequestParam Long registrationId) {
        return ResponseEntity.ok(ausenciaService.listarPorInscricao(registrationId));
    }

    @GetMapping("/por-contexto")
    public ResponseEntity<List<AusenciaTomadaSeguidorLinhaDTO>> listarPorContexto(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(ausenciaService.listarPorContexto(competitionId, categoryId));
    }
}
