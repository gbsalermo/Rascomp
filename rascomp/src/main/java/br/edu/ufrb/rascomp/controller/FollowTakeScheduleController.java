package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.FollowTakeScheduleDTO;
import br.edu.ufrb.rascomp.dto.FollowTakeScheduleEntryDTO;
import br.edu.ufrb.rascomp.service.FollowTakeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/agenda-follow")
@RequiredArgsConstructor
public class FollowTakeScheduleController {

    private final FollowTakeScheduleService service;

    @PostMapping
    public ResponseEntity<FollowTakeScheduleDTO> criar(
            @Valid @RequestBody FollowTakeScheduleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FollowTakeScheduleDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody FollowTakeScheduleDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FollowTakeScheduleDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<FollowTakeScheduleDTO>> listarPorCompeticao(
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(service.listarPorCompeticao(competitionId));
    }

    @GetMapping("/por-categoria")
    public ResponseEntity<List<FollowTakeScheduleDTO>> listarPorCategoria(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(service.listarPorCategoria(competitionId, categoryId));
    }

    @GetMapping("/{id}/fila")
    public ResponseEntity<List<FollowTakeScheduleEntryDTO>> fila(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarFila(id));
    }

    @PostMapping("/{id}/sincronizar-fila")
    public ResponseEntity<List<FollowTakeScheduleEntryDTO>> sincronizarFila(@PathVariable Long id) {
        return ResponseEntity.ok(service.sincronizarFila(id));
    }

    @PatchMapping("/fila/{entryId}")
    public ResponseEntity<FollowTakeScheduleEntryDTO> atualizarConvocacao(
            @PathVariable Long entryId,
            @Valid @RequestBody FollowTakeScheduleEntryDTO dto) {
        return ResponseEntity.ok(service.atualizarConvocacao(entryId, dto));
    }
}
