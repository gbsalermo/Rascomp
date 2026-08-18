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

import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.service.RoundSumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rounds-sumo")
@RequiredArgsConstructor
public class RoundSumoController {

    private final RoundSumoService roundSumoService;

    @PostMapping
    public ResponseEntity<RoundSumoDTO> registrar(@Valid @RequestBody RoundSumoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roundSumoService.registrar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoundSumoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(roundSumoService.buscarPorId(id));
    }

    @GetMapping("/por-partida")
    public ResponseEntity<List<RoundSumoDTO>> listarPorPartida(@RequestParam Long matchId) {
        return ResponseEntity.ok(roundSumoService.listarPorPartida(matchId));
    }
}
