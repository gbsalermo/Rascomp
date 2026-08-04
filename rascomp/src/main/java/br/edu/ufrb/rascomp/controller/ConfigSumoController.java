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

import br.edu.ufrb.rascomp.dto.ConfigSumoDTO;
import br.edu.ufrb.rascomp.service.ConfigSumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(
    "/api/v1/categorias/{categoryId}/config-sumo"
)
@RequiredArgsConstructor
public class ConfigSumoController {

    private final ConfigSumoService configSumoService;

    @PostMapping
    public ResponseEntity<ConfigSumoDTO> criar(
            @PathVariable Long categoryId,
            @Valid @RequestBody ConfigSumoDTO dto) {

        ConfigSumoDTO configCriada =
                configSumoService.criar(
                        categoryId,
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(configCriada);
    }

    @GetMapping
    public ResponseEntity<ConfigSumoDTO>
            buscarPorCategoria(
                    @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                configSumoService
                        .buscarPorCategoria(categoryId)
        );
    }

    @PutMapping
    public ResponseEntity<ConfigSumoDTO> atualizar(
            @PathVariable Long categoryId,
            @Valid @RequestBody ConfigSumoDTO dto) {

        return ResponseEntity.ok(
                configSumoService.atualizar(
                        categoryId,
                        dto
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deletar(
            @PathVariable Long categoryId) {

        configSumoService.deletar(categoryId);

        return ResponseEntity
                .noContent()
                .build();
    }
}