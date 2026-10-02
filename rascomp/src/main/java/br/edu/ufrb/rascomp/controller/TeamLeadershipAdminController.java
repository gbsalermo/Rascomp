package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.TeamLeaderTransferRequest;
import br.edu.ufrb.rascomp.dto.TeamLeadershipHistoryDTO;
import br.edu.ufrb.rascomp.service.TeamLeadershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/equipes/{teamId}/lideranca")
@RequiredArgsConstructor
public class TeamLeadershipAdminController {

    private final TeamLeadershipService service;

    @GetMapping("/candidatos")
    public ResponseEntity<List<CompetitorDTO>> candidatos(
            @PathVariable Long teamId,
            @RequestParam Long competitionId) {
        return ResponseEntity.ok(service.candidatos(teamId, competitionId));
    }

    @PatchMapping
    public ResponseEntity<TeamLeadershipHistoryDTO> transferir(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamLeaderTransferRequest request) {
        return ResponseEntity.ok(service.transferir(teamId, request));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<TeamLeadershipHistoryDTO>> historico(@PathVariable Long teamId) {
        return ResponseEntity.ok(service.historico(teamId));
    }
}
