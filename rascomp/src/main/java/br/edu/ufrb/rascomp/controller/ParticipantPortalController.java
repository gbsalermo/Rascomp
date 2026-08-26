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
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitorRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRobotRequest;
import br.edu.ufrb.rascomp.dto.ParticipantTeamRequest;
import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.service.ParticipantPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/participante")
@RequiredArgsConstructor
public class ParticipantPortalController {

    private final ParticipantPortalService portalService;

    @GetMapping("/equipes")
    public ResponseEntity<List<TeamDTO>> equipes() {
        return ResponseEntity.ok(portalService.minhasEquipes());
    }

    @PostMapping("/equipes")
    public ResponseEntity<TeamDTO> criarEquipe(@Valid @RequestBody ParticipantTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.criarEquipe(request));
    }

    @PutMapping("/equipes/{teamId}")
    public ResponseEntity<TeamDTO> atualizarEquipe(
            @PathVariable Long teamId,
            @Valid @RequestBody ParticipantTeamRequest request) {
        return ResponseEntity.ok(portalService.atualizarEquipe(teamId, request));
    }

    @GetMapping("/equipes/{teamId}/competidores")
    public ResponseEntity<List<CompetitorDTO>> competidores(@PathVariable Long teamId) {
        return ResponseEntity.ok(portalService.competidores(teamId));
    }

    @PostMapping("/equipes/{teamId}/competidores")
    public ResponseEntity<CompetitorDTO> criarCompetidor(
            @PathVariable Long teamId,
            @Valid @RequestBody ParticipantCompetitorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.criarCompetidor(teamId, request));
    }

    @PostMapping("/equipes/{teamId}/competidores/eu")
    public ResponseEntity<CompetitorDTO> tornarMeCompetidor(@PathVariable Long teamId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.tornarMeCompetidor(teamId));
    }

    @PutMapping("/competidores/{competitorId}")
    public ResponseEntity<CompetitorDTO> atualizarCompetidor(
            @PathVariable Long competitorId,
            @Valid @RequestBody ParticipantCompetitorRequest request) {
        return ResponseEntity.ok(portalService.atualizarCompetidor(competitorId, request));
    }

    @DeleteMapping("/competidores/{competitorId}")
    public ResponseEntity<Void> removerCompetidor(@PathVariable Long competitorId) {
        portalService.removerCompetidor(competitorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/equipes/{teamId}/robos")
    public ResponseEntity<List<RobotDTO>> robos(@PathVariable Long teamId) {
        return ResponseEntity.ok(portalService.robos(teamId));
    }

    @PostMapping("/equipes/{teamId}/robos")
    public ResponseEntity<RobotDTO> criarRobo(
            @PathVariable Long teamId,
            @Valid @RequestBody ParticipantRobotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.criarRobo(teamId, request));
    }

    @PutMapping("/robos/{robotId}")
    public ResponseEntity<RobotDTO> atualizarRobo(
            @PathVariable Long robotId,
            @Valid @RequestBody ParticipantRobotRequest request) {
        return ResponseEntity.ok(portalService.atualizarRobo(robotId, request));
    }

    @DeleteMapping("/robos/{robotId}")
    public ResponseEntity<Void> removerRobo(@PathVariable Long robotId) {
        portalService.removerRobo(robotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/robos/{robotId}/fotos")
    public ResponseEntity<List<RobotImageDTO>> fotos(@PathVariable Long robotId) {
        return ResponseEntity.ok(portalService.fotos(robotId));
    }

    @PostMapping(value = "/robos/{robotId}/fotos", consumes = "multipart/form-data")
    public ResponseEntity<RobotImageDTO> adicionarFoto(
            @PathVariable Long robotId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.adicionarFoto(robotId, arquivo));
    }

    @PatchMapping("/robos/{robotId}/fotos/{imageId}/principal")
    public ResponseEntity<RobotImageDTO> definirPrincipal(
            @PathVariable Long robotId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(portalService.definirFotoPrincipal(robotId, imageId));
    }

    @DeleteMapping("/robos/{robotId}/fotos/{imageId}")
    public ResponseEntity<Void> removerFoto(
            @PathVariable Long robotId,
            @PathVariable Long imageId) {
        portalService.removerFoto(robotId, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/equipes/{teamId}/inscricoes")
    public ResponseEntity<List<RegistrationDTO>> inscricoes(@PathVariable Long teamId) {
        return ResponseEntity.ok(portalService.inscricoes(teamId));
    }

    @GetMapping("/inscricoes/{registrationId}/tentativas-follow")
    public ResponseEntity<List<TentativaSeguidorLinhaDTO>> tentativasFollow(@PathVariable Long registrationId) {
        return ResponseEntity.ok(portalService.tentativasFollow(registrationId));
    }

    @PostMapping("/equipes/{teamId}/inscricoes")
    public ResponseEntity<RegistrationDTO> inscrever(
            @PathVariable Long teamId,
            @Valid @RequestBody ParticipantRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.inscrever(teamId, request));
    }

    @DeleteMapping("/inscricoes/{registrationId}")
    public ResponseEntity<Void> cancelarInscricao(@PathVariable Long registrationId) {
        portalService.cancelarInscricao(registrationId);
        return ResponseEntity.noContent().build();
    }
}
