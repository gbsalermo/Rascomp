package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotImageRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicQueryService {

    private final CompetitionService competitionService;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotImageRepository robotImageRepository;
    private final RegistrationRepository registrationRepository;
    private final RankingFollowService rankingFollowService;
    private final BracketService bracketService;
    private final MatchService matchService;
    private final MatchResultService matchResultService;
    private final RobotImageService robotImageService;

    public List<CompetitionDTO> competicoes() {
        return competitionService.listar(true);
    }

    @Transactional(readOnly = true)
    public List<PublicTeamDTO> equipes() {
        return teamRepository.findByAtivoTrueOrderByNomeAsc()
                .stream().map(PublicTeamDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCompetitorDTO> competidores(Long teamId) {
        return competitorRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId)
                .stream().map(PublicCompetitorDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicRobotDTO> robos(Long teamId) {
        List<Robot> robots = teamId == null
                ? robotRepository.findByAtivoTrueOrderByNomeAsc()
                : robotRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId);

        return robots.stream()
                .map(robot -> new PublicRobotDTO(robot, fotoPrincipal(robot.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RobotImageDTO> fotos(Long robotId) {
        return robotImageService.listar(robotId);
    }

    @Transactional(readOnly = true)
    public List<PublicRegistrationDTO> inscricoes(Long competitionId) {
        return registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream()
                .filter(registration -> Boolean.TRUE.equals(registration.getAtivo()))
                .filter(registration -> registration.getStatus() == StatusRegistration.APROVADA)
                .map(PublicRegistrationDTO::new)
                .toList();
    }

    public List<RankingFollowDTO> rankingFollow(Long competitionId, Long categoryId) {
        return rankingFollowService.gerarRanking(competitionId, categoryId);
    }

    public List<BracketDTO> chaveamentos(Long competitionId) {
        return bracketService.listarPorCompeticao(competitionId);
    }

    public List<MatchDTO> partidas(Long bracketId) {
        return matchService.listarPorChaveamento(bracketId);
    }

    public List<MatchResultDTO> resultados(Long bracketId) {
        return matchResultService.listarPorChaveamento(bracketId);
    }

    public RobotImageService.RobotImageFile arquivoFoto(Long robotId, Long imageId) {
        return robotImageService.carregarPublico(robotId, imageId);
    }

    private String fotoPrincipal(Long robotId) {
        return robotImageRepository.findFirstByRobotIdAndPrincipalTrueAndAtivoTrue(robotId)
                .map(image -> "/api/v1/public/robos/" + robotId + "/fotos/" + image.getId() + "/arquivo")
                .orElse(null);
    }
}
