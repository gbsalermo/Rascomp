package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.service.InspecaoSumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inspecoes-sumo")
@RequiredArgsConstructor
public class InspecaoSumoController {

    private final InspecaoSumoService inspecaoSumoService;

    @PostMapping
    public ResponseEntity<InspecaoSumoDTO> registrar(
            @Valid @RequestBody InspecaoSumoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspecaoSumoService.registrar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspecaoSumoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(inspecaoSumoService.buscarPorId(id));
    }

    @GetMapping("/por-inscricao")
    public ResponseEntity<List<InspecaoSumoDTO>> listarPorInscricao(
            @RequestParam Long registrationId) {
        return ResponseEntity.ok(inspecaoSumoService.listarPorInscricao(registrationId));
    }

    @GetMapping("/ultima")
    public ResponseEntity<InspecaoSumoDTO> buscarUltimaPorInscricao(
            @RequestParam Long registrationId) {
        return ResponseEntity.ok(inspecaoSumoService.buscarUltimaPorInscricao(registrationId));
    }

    @GetMapping("/aptidao")
    public ResponseEntity<Boolean> verificarAptidao(
            @RequestParam Long registrationId) {
        return ResponseEntity.ok(inspecaoSumoService.estaAptaParaCompetir(registrationId));
    }
}
