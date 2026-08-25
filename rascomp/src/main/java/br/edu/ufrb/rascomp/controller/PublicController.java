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
import br.edu.ufrb.rascomp.dto.CompetitionCategoryDTO;
import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.dto.InstitutionDTO;
import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.dto.PublicCompetitorDTO;
import br.edu.ufrb.rascomp.dto.PublicRegistrationDTO;
import br.edu.ufrb.rascomp.dto.PublicRobotDTO;
import br.edu.ufrb.rascomp.dto.PublicTeamDTO;
import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.service.PublicQueryService;
import br.edu.ufrb.rascomp.service.RobotImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "API Pública")
public class PublicController {

    private final PublicQueryService publicQueryService;

    @GetMapping("/competicoes")
    @Operation(summary = "Listar competições públicas")
    public ResponseEntity<List<CompetitionDTO>> competicoes() {
        return ResponseEntity.ok(publicQueryService.competicoes());
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorias públicas")
    public ResponseEntity<List<CompetitionCategoryDTO>> categorias(
            @RequestParam(required = false) Modalidade modalidade) {
        return ResponseEntity.ok(publicQueryService.categorias(modalidade));
    }

    @GetMapping("/instituicoes")
    @Operation(summary = "Listar instituições públicas")
    public ResponseEntity<List<InstitutionDTO>> instituicoes() {
        return ResponseEntity.ok(publicQueryService.instituicoes());
    }

    @GetMapping("/equipes")
    @Operation(summary = "Listar equipes públicas")
    public ResponseEntity<List<PublicTeamDTO>> equipes() {
        return ResponseEntity.ok(publicQueryService.equipes());
    }

    @GetMapping("/competidores")
    @Operation(summary = "Listar competidores públicos por equipe")
    public ResponseEntity<List<PublicCompetitorDTO>> competidores(@RequestParam Long teamId) {
        return ResponseEntity.ok(publicQueryService.competidores(teamId));
    }

    @GetMapping("/robos")
    @Operation(summary = "Listar robôs públicos")
    public ResponseEntity<List<PublicRobotDTO>> robos(@RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(publicQueryService.robos(teamId));
    }

    @GetMapping("/robos/{robotId}/fotos")
    @Operation(summary = "Listar fotos públicas de um robô")
    public ResponseEntity<List<RobotImageDTO>> fotos(@PathVariable Long robotId) {
        return ResponseEntity.ok(publicQueryService.fotos(robotId));
    }

    @GetMapping("/robos/{robotId}/fotos/{imageId}/arquivo")
    @Operation(summary = "Obter arquivo público de uma foto do robô")
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
    @Operation(summary = "Listar inscrições públicas de uma competição")
    public ResponseEntity<List<PublicRegistrationDTO>> inscricoes(@RequestParam Long competitionId) {
        return ResponseEntity.ok(publicQueryService.inscricoes(competitionId));
    }

    @GetMapping("/ranking/seguidor-linha")
    @Operation(summary = "Consultar ranking público do Seguidor de Linha")
    public ResponseEntity<List<RankingFollowDTO>> rankingFollow(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(publicQueryService.rankingFollow(competitionId, categoryId));
    }

    @GetMapping("/chaveamentos")
    @Operation(summary = "Listar chaveamentos públicos do Sumô")
    public ResponseEntity<List<BracketDTO>> chaveamentos(@RequestParam Long competitionId) {
        return ResponseEntity.ok(publicQueryService.chaveamentos(competitionId));
    }

    @GetMapping("/partidas")
    @Operation(summary = "Listar partidas públicas de um chaveamento")
    public ResponseEntity<List<MatchDTO>> partidas(@RequestParam Long bracketId) {
        return ResponseEntity.ok(publicQueryService.partidas(bracketId));
    }

    @GetMapping("/resultados")
    @Operation(summary = "Listar resultados públicos de um chaveamento")
    public ResponseEntity<List<MatchResultDTO>> resultados(@RequestParam Long bracketId) {
        return ResponseEntity.ok(publicQueryService.resultados(bracketId));
    }
}
