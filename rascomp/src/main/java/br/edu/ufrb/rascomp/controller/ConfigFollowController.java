package br.edu.ufrb.rascomp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.ConfigFollowDTO;
import br.edu.ufrb.rascomp.service.ConfigFollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
    "/api/v1/categorias/{categoryId}/config-follow"
)
@RequiredArgsConstructor
public class ConfigFollowController {

    private final ConfigFollowService configFollowService;

    @PostMapping
    public ResponseEntity<ConfigFollowDTO> criar(
            @PathVariable Long categoryId,
            @Valid @RequestBody ConfigFollowDTO dto) {

        ConfigFollowDTO configCriada =
                configFollowService.criar(
                        categoryId,
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(configCriada);
    }

    @GetMapping
    public ResponseEntity<ConfigFollowDTO>
            buscarPorCategoria(
                    @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                configFollowService
                        .buscarPorCategoria(categoryId)
        );
    }

    @PutMapping
    public ResponseEntity<ConfigFollowDTO> atualizar(
            @PathVariable Long categoryId,
            @Valid @RequestBody ConfigFollowDTO dto) {

        return ResponseEntity.ok(
                configFollowService.atualiza(categoryId, dto)
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deletar(
            @PathVariable Long categoryId) {

        configFollowService.deletar(categoryId);

        return ResponseEntity
                .noContent()
                .build();
    }
}