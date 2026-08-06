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

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inscricoes")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationDTO> criar(@Valid @RequestBody RegistrationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<RegistrationDTO>> listar(@RequestParam(defaultValue = "false") boolean apenasAtivas) {
        return ResponseEntity.ok(registrationService.listar(apenasAtivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.buscarPorId(id));
    }

    @GetMapping("/por-competicao")
    public ResponseEntity<List<RegistrationDTO>> listarPorCompeticao(@RequestParam Long competitionId) {
        return ResponseEntity.ok(registrationService.listarPorCompeticao(competitionId));
    }

    @GetMapping("/por-status")
    public ResponseEntity<List<RegistrationDTO>> listarPorStatus(@RequestParam StatusRegistration status) {
        return ResponseEntity.ok(registrationService.listarPorStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RegistrationDTO> atualizar(@PathVariable Long id, @Valid @RequestBody RegistrationDTO dto) {
        return ResponseEntity.ok(registrationService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        registrationService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<RegistrationDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.reativar(id));
    }
}
