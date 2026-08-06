package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.service.RobotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/robos")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    @PostMapping
    public ResponseEntity<RobotDTO> criar(
            @Valid @RequestBody RobotDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(robotService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<RobotDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {

        return ResponseEntity.ok(
                apenasAtivos
                        ? robotService.listarAtivos()
                        : robotService.listarTodos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RobotDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                robotService.buscarPorId(id)
        );
    }

    @GetMapping("/por-equipe")
    public ResponseEntity<List<RobotDTO>> listarPorEquipe(
            @RequestParam Long teamId,
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {

        return ResponseEntity.ok(
                robotService.listarPorEquipe(teamId, apenasAtivos)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<RobotDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RobotDTO dto) {

        return ResponseEntity.ok(
                robotService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id) {

        robotService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<RobotDTO> reativar(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                robotService.reativar(id)
        );
    }
}