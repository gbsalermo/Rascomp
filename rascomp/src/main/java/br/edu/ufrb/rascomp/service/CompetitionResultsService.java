package br.edu.ufrb.rascomp.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionCategoryResultDTO;
import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionResultsService {

    private final RegistrationRepository registrationRepository;
    private final BracketRepository bracketRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final RankingFollowService rankingFollowService;
    private final ConfigFollowRepository configFollowRepository;
    private final TentativaSeguidorLinhaRepository tentativaFollowRepository;
    private final AusenciaTomadaSeguidorLinhaRepository ausenciaFollowRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional(readOnly = true)
    public List<CompetitionCategoryResultDTO> listar(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);

        Map<Long, CompetitionCategory> categorias = new LinkedHashMap<>();
        for (Registration registration :
                registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)) {
            categorias.putIfAbsent(
                    registration.getCategory().getId(),
                    registration.getCategory());
        }

        return categorias.values().stream()
                .filter(category -> Boolean.TRUE.equals(category.getAtivo()))
                .sorted(Comparator.comparing(CompetitionCategory::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(category -> category.getModalidade() == Modalidade.FOLLOW_LINE
                        ? followResult(competitionId, category)
                        : sumoResult(competitionId, category))
                .toList();
    }

    private CompetitionCategoryResultDTO followResult(
            Long competitionId,
            CompetitionCategory category) {

        CompetitionCategoryResultDTO dto = base(category);
        List<RankingFollowDTO> ranking =
                rankingFollowService.gerarRanking(competitionId, category.getId());

        if (ranking.isEmpty() || !programaFollowConcluido(competitionId, category.getId())) {
            return dto;
        }

        RankingFollowDTO winner = ranking.get(0);
        dto.setStatus("CONCLUIDO");
        dto.setWinnerRegistrationId(winner.getRegistrationId());
        dto.setWinnerRobotNome(winner.getRobotNome());
        dto.setWinnerTeamNome(winner.getTeamNome());
        dto.setTempoFinalSegundos(winner.getTempoFinalSegundos());
        return dto;
    }

    private boolean programaFollowConcluido(Long competitionId, Long categoryId) {
        ConfigFollow config = configFollowRepository.findByCompetitionCategoryId(categoryId)
                .orElse(null);
        if (config == null) return false;

        List<Registration> participantes = registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        competitionId,
                        categoryId,
                        br.edu.ufrb.rascomp.model.Enum.StatusRegistration.APROVADA);

        if (participantes.isEmpty()) return false;

        for (Registration registration : participantes) {
            for (int tomada = 1; tomada <= config.getNumeroTomadas(); tomada++) {
                boolean ausente = ausenciaFollowRepository
                        .existsByRegistrationIdAndTomada(registration.getId(), tomada);
                long tentativas = tentativaFollowRepository
                        .countByRegistrationIdAndTomada(registration.getId(), tomada);

                if (!ausente && tentativas < config.getTentativasPorTomada()) {
                    return false;
                }
            }
        }

        return true;
    }

    private CompetitionCategoryResultDTO sumoResult(
            Long competitionId,
            CompetitionCategory category) {

        CompetitionCategoryResultDTO dto = base(category);

        Bracket bracket = bracketRepository
                .findByCompetitionIdAndCategoryIdAndAtualTrue(competitionId, category.getId())
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .findFirst()
                .orElse(null);

        if (bracket == null) return dto;

        List<Match> matches =
                matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId());
        Integer ultimaRodada = matches.stream()
                .map(Match::getRodada)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        if (ultimaRodada == null) return dto;

        Match finalMatch = matches.stream()
                .filter(item -> ultimaRodada.equals(item.getRodada()))
                .min(Comparator.comparing(Match::getOrdem))
                .orElse(null);

        if (finalMatch == null) return dto;

        MatchResult result = matchResultRepository.findByMatchId(finalMatch.getId())
                .orElse(null);
        if (result == null || result.getWinner() == null) return dto;

        dto.setStatus("CONCLUIDO");
        dto.setWinnerRegistrationId(result.getWinner().getId());
        dto.setWinnerRobotNome(result.getWinner().getRobot().getNome());
        dto.setWinnerTeamNome(result.getWinner().getTeam().getNome());
        dto.setPontosA(result.getPontosA());
        dto.setPontosB(result.getPontosB());
        dto.setFinalMatchId(finalMatch.getId());
        return dto;
    }

    private CompetitionCategoryResultDTO base(CompetitionCategory category) {
        CompetitionCategoryResultDTO dto = new CompetitionCategoryResultDTO();
        dto.setCategoryId(category.getId());
        dto.setCategoryNome(category.getNome());
        dto.setModalidade(category.getModalidade());
        dto.setStatus("PENDENTE");
        return dto;
    }
}
