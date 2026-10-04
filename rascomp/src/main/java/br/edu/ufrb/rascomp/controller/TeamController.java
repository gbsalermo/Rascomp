package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/equipes")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<TeamDTO> criar(
            @Valid @RequestBody TeamDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.criar(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<List<TeamDTO>> listar(
            @RequestParam(
                defaultValue = "false"
            ) boolean apenasAtivas) {

        if (apenasAtivas) {
            return ResponseEntity.ok(
                    teamService.listarAtivas()
            );
        }

        return ResponseEntity.ok(
                teamService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<TeamDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teamService.buscarPorId(id)
        );
    }

    @GetMapping("/por-instituicao")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<List<TeamDTO>> listarPorInstituicao(
            @RequestParam Long institutionId,
            @RequestParam(
                defaultValue = "false"
            ) boolean apenasAtivas) {

        return ResponseEntity.ok(
                teamService.listarPorInstituicao(
                        institutionId,
                        apenasAtivas
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<TeamDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody TeamDTO dto) {

        return ResponseEntity.ok(
                teamService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        teamService.deletar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<TeamDTO> reativar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teamService.reativar(id)
        );
    }
}