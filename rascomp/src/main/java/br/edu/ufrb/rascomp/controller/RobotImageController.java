package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.service.RobotImageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/robos/{robotId}/fotos")
@RequiredArgsConstructor
public class RobotImageController {

    private final RobotImageService robotImageService;

    @GetMapping
    public ResponseEntity<List<RobotImageDTO>> listar(@PathVariable Long robotId) {
        return ResponseEntity.ok(robotImageService.listar(robotId));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<RobotImageDTO> adicionar(
            @PathVariable Long robotId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(robotImageService.adicionar(robotId, arquivo));
    }

    @PatchMapping("/{imageId}/principal")
    public ResponseEntity<RobotImageDTO> principal(
            @PathVariable Long robotId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(robotImageService.definirPrincipal(robotId, imageId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> remover(
            @PathVariable Long robotId,
            @PathVariable Long imageId) {
        robotImageService.remover(robotId, imageId);
        return ResponseEntity.noContent().build();
    }
}
