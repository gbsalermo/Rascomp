package br.edu.ufrb.rascomp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.service.RankingFollowService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ranking/seguidor-linha")
@RequiredArgsConstructor
public class RankingFollowController {

    private final RankingFollowService rankingFollowService;

    @GetMapping
    public ResponseEntity<List<RankingFollowDTO>> gerarRanking(
            @RequestParam Long competitionId,
            @RequestParam Long categoryId) {

        return ResponseEntity.ok(rankingFollowService.gerarRanking(competitionId, categoryId));
    }
}
