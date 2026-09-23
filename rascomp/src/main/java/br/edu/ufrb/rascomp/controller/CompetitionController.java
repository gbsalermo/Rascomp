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

import br.edu.ufrb.rascomp.dto.CompetitionAdminCatalogDTO;
import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.dto.CompetitionRegistrationWindowChangeDTO;
import br.edu.ufrb.rascomp.dto.CompetitionRegistrationWindowChangeRequest;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.service.CompetitionAdminCatalogService;
import br.edu.ufrb.rascomp.service.CompetitionContextService;
import br.edu.ufrb.rascomp.service.CompetitionRegistrationWindowService;
import br.edu.ufrb.rascomp.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/competicoes")
@RequiredArgsConstructor
public class CompetitionController {
    private final CompetitionService competitionService;
    private final CompetitionContextService competitionContextService;
    private final CompetitionAdminCatalogService competitionAdminCatalogService;
    private final CompetitionRegistrationWindowService registrationWindowService;

    @PostMapping
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<CompetitionDTO> criar(@Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitionService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompetitionDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivas) {
        return ResponseEntity.ok(competitionContextService.listarVisiveis(apenasAtivas));
    }

    @GetMapping("/por-status")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<List<CompetitionDTO>> listarPorStatus(@RequestParam StatusCompetition status) {
        return ResponseEntity.ok(competitionService.listarPorStatus(status));
    }

    @GetMapping("/vigente")
    public ResponseEntity<CompetitionDTO> vigente() {
        CompetitionDTO vigente = competitionContextService.buscarVigente();
        return vigente == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(vigente);
    }

    @PatchMapping("/{id}/vigente")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<CompetitionDTO> definirVigente(@PathVariable Long id) {
        return ResponseEntity.ok(competitionContextService.definirVigente(id));
    }

    @GetMapping("/{id}/catalogo-administrativo")
    public ResponseEntity<CompetitionAdminCatalogDTO> catalogoAdministrativo(@PathVariable Long id) {
        return ResponseEntity.ok(competitionAdminCatalogService.buscar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(competitionContextService.buscarVisivel(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<CompetitionDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.ok(competitionService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompetitionDTO> alterarStatus(
            @PathVariable Long id,
            @RequestParam StatusCompetition status) {
        competitionContextService.exigirPodeAlterarStatus(id, status);
        return ResponseEntity.ok(competitionService.alterarStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        competitionService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<CompetitionDTO> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.reativar(id));
    }

    @PostMapping("/{id}/prorrogar-inscricoes")
    public ResponseEntity<CompetitionRegistrationWindowChangeDTO> prorrogarInscricoes(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionRegistrationWindowChangeRequest request) {
        return ResponseEntity.ok(registrationWindowService.alterar(id, request));
    }

    @GetMapping("/{id}/historico-inscricoes")
    public ResponseEntity<List<CompetitionRegistrationWindowChangeDTO>> historicoInscricoes(@PathVariable Long id) {
        return ResponseEntity.ok(registrationWindowService.historico(id));
    }
}
