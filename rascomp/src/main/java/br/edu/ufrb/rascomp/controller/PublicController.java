package br.edu.ufrb.rascomp.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.dto.PublicCompetitorDTO;
import br.edu.ufrb.rascomp.dto.PublicRegistrationDTO;
import br.edu.ufrb.rascomp.dto.PublicRobotDTO;
import br.edu.ufrb.rascomp.dto.PublicTeamDTO;
import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.service.PublicQueryService;
import br.edu.ufrb.rascomp.service.RobotImageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicQueryService publicQueryService;

    @GetMapping("/competicoes")
    public ResponseEntity<List<CompetitionDTO>> competicoes() {
        return ResponseEntity.ok(publicQueryService.competicoes());
    }

    @GetMapping("/equipes")
    public ResponseEntity<List<PublicTeamDTO>> equipes() {
        return ResponseEntity.ok(publicQueryService.equipes());
    }

    @GetMapping("/competidores")
    public ResponseEntity<List<PublicCompetitorDTO>> competidores(@RequestParam Long teamId) {
        return ResponseEntity.ok(publicQueryService.competidores(teamId));
    }

    @GetMapping("/robos")
    public ResponseEntity<List<PublicRobotDTO>> robos(@RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(publicQueryService.robos(teamId));
    }

    @GetMapping("/robos/{robotId}/fotos")
    public ResponseEntity<List<RobotImageDTO>> fotos(@PathVariable Long robotId) {
        return ResponseEntity.ok(publicQueryService.fotos(robotId));
    }

    @GetMapping("/robos/{robotId}/fotos/{imageId}/arquivo")
    public ResponseEntity<Resource> arquivoFoto(
            @PathVariable Long robotId,
            @PathVariable Long imageId) {
        RobotImageService.RobotImageFile file = publicQueryService.arquivoFoto(robotId, imageId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @GetMapping("/inscricoes")
    public ResponseEntity<List<PublicRegistrationDTO>> inscricoes(@RequestParam Long competitionId) {
        return ResponseEntity.ok(publicQueryService.inscricoes(competitionId));
    }

    @GetMapping("/ranking/seguidor-linha")
    public ResponseEntity<List<RankingFollowDTO>> rankingFollow(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(publicQueryService.rankingFollow(competitionId, categoryId));
    }

    @GetMapping("/chaveamentos")
    public ResponseEntity<List<BracketDTO>> chaveamentos(@RequestParam Long competitionId) {
        return ResponseEntity.ok(publicQueryService.chaveamentos(competitionId));
    }

    @GetMapping("/partidas")
    public ResponseEntity<List<MatchDTO>> partidas(@RequestParam Long bracketId) {
        return ResponseEntity.ok(publicQueryService.partidas(bracketId));
    }

    @GetMapping("/resultados")
    public ResponseEntity<List<MatchResultDTO>> resultados(@RequestParam Long bracketId) {
        return ResponseEntity.ok(publicQueryService.resultados(bracketId));
    }
}
