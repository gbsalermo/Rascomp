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
import br.edu.ufrb.rascomp.model.FollowManualResult;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.TipoPartidaSumo;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionResultsService {

    private final RegistrationRepository registrationRepository;
    private final CompetitionRepository competitionRepository;
    private final BracketRepository bracketRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final RankingFollowService rankingFollowService;
    private final FollowResolutionService followResolutionService;
    private final FollowManualResultRepository followManualResultRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional(readOnly = true)
    public List<CompetitionCategoryResultDTO> listar(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        return consolidar(competitionId);
    }

    @Transactional(readOnly = true)
    public List<CompetitionCategoryResultDTO> listarPublico(Long competitionId) {
        var competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Competição não encontrada com o id: " + competitionId));

        boolean statusPublico = switch (competition.getStatus()) {
            case INSCRICOES_ABERTAS, INSCRICOES_ENCERRADAS, EM_ANDAMENTO -> true;
            default -> false;
        };

        if (!Boolean.TRUE.equals(competition.getAtivo()) || !statusPublico) {
            throw new jakarta.persistence.EntityNotFoundException(
                    "Resultados públicos indisponíveis para a competição: " + competitionId);
        }

        return consolidar(competitionId);
    }

    private List<CompetitionCategoryResultDTO> consolidar(Long competitionId) {
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
        Long categoryId = category.getId();

        dto.setExtraTakeNumber(followResolutionService.numeroTomadaExtra(categoryId));
        dto.setExtraTakeActive(
                followResolutionService.tomadaExtraAtiva(competitionId, categoryId));
        dto.setExtraTakeAvailable(
                followResolutionService.podeCriarTomadaExtra(competitionId, categoryId));
        dto.setManualDecisionAvailable(
                followResolutionService.podeDecidirManualmente(competitionId, categoryId));

        FollowManualResult manual = followManualResultRepository
                .findByCompetitionIdAndCategoryId(competitionId, categoryId)
                .orElse(null);

        if (manual != null) {
            dto.setStatus("CONCLUIDO");
            dto.setWinnerRegistrationId(manual.getWinnerRegistration().getId());
            dto.setWinnerRobotNome(manual.getWinnerRegistration().getRobot().getNome());
            dto.setWinnerTeamNome(manual.getWinnerRegistration().getTeam().getNome());
            if (manual.getSecondRegistration() != null) {
                dto.setSecondRegistrationId(manual.getSecondRegistration().getId());
                dto.setSecondRobotNome(manual.getSecondRegistration().getRobot().getNome());
                dto.setSecondTeamNome(manual.getSecondRegistration().getTeam().getNome());
            }
            if (manual.getThirdRegistration() != null) {
                dto.setThirdRegistrationId(manual.getThirdRegistration().getId());
                dto.setThirdRobotNome(manual.getThirdRegistration().getRobot().getNome());
                dto.setThirdTeamNome(manual.getThirdRegistration().getTeam().getNome());
            }
            dto.setResolutionType("DECISAO_ORGANIZACAO");
            dto.setResolutionReason(manual.getJustificativa());
            dto.setResolutionActorNome(manual.getDecidedByUser().getNome());
            dto.setResolutionAt(manual.getDataCadastro());
            dto.setExtraTakeAvailable(false);
            dto.setManualDecisionAvailable(false);
            return dto;
        }

        List<RankingFollowDTO> ranking =
                rankingFollowService.gerarRanking(competitionId, categoryId);

        if (ranking.isEmpty()
                || !followResolutionService.programaConcluido(competitionId, categoryId)) {
            return dto;
        }

        RankingFollowDTO winner = ranking.get(0);
        dto.setStatus("CONCLUIDO");
        dto.setWinnerRegistrationId(winner.getRegistrationId());
        dto.setWinnerRobotNome(winner.getRobotNome());
        dto.setWinnerTeamNome(winner.getTeamNome());
        dto.setTempoFinalSegundos(winner.getTempoFinalSegundos());

        if (ranking.size() > 1) {
            RankingFollowDTO second = ranking.get(1);
            dto.setSecondRegistrationId(second.getRegistrationId());
            dto.setSecondRobotNome(second.getRobotNome());
            dto.setSecondTeamNome(second.getTeamNome());
            dto.setSecondTempoFinalSegundos(second.getTempoFinalSegundos());
        }
        if (ranking.size() > 2) {
            RankingFollowDTO third = ranking.get(2);
            dto.setThirdRegistrationId(third.getRegistrationId());
            dto.setThirdRobotNome(third.getRobotNome());
            dto.setThirdTeamNome(third.getTeamNome());
            dto.setThirdTempoFinalSegundos(third.getTempoFinalSegundos());
        }

        dto.setResolutionType("RANKING");
        dto.setExtraTakeAvailable(false);
        dto.setManualDecisionAvailable(false);
        return dto;
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

        Match finalMatch = matches.stream()
                .filter(item -> item.getTipoPartida() != TipoPartidaSumo.TERCEIRO_LUGAR)
                .max(Comparator.comparing(Match::getRodada)
                        .thenComparing(item -> -item.getOrdem()))
                .orElse(null);

        if (finalMatch == null) return dto;

        MatchResult finalResult = matchResultRepository.findByMatchId(finalMatch.getId())
                .orElse(null);
        if (finalResult == null || finalResult.getWinner() == null) return dto;

        Registration champion = finalResult.getWinner();
        Registration vice = finalMatch.getRegistrationA() != null
                && finalMatch.getRegistrationA().getId().equals(champion.getId())
                ? finalMatch.getRegistrationB()
                : finalMatch.getRegistrationA();

        dto.setWinnerRegistrationId(champion.getId());
        dto.setWinnerRobotNome(champion.getRobot().getNome());
        dto.setWinnerTeamNome(champion.getTeam().getNome());
        if (vice != null) {
            dto.setSecondRegistrationId(vice.getId());
            dto.setSecondRobotNome(vice.getRobot().getNome());
            dto.setSecondTeamNome(vice.getTeam().getNome());
        }
        dto.setPontosA(finalResult.getPontosA());
        dto.setPontosB(finalResult.getPontosB());
        dto.setFinalMatchId(finalMatch.getId());

        Match thirdMatch = matches.stream()
                .filter(item -> item.getTipoPartida() == TipoPartidaSumo.TERCEIRO_LUGAR)
                .findFirst()
                .orElse(null);

        if (thirdMatch != null) {
            dto.setThirdPlaceMatchId(thirdMatch.getId());
            MatchResult thirdResult = matchResultRepository.findByMatchId(thirdMatch.getId())
                    .orElse(null);
            if (thirdResult == null || thirdResult.getWinner() == null) {
                dto.setStatus("PARCIAL");
                return dto;
            }
            dto.setThirdRegistrationId(thirdResult.getWinner().getId());
            dto.setThirdRobotNome(thirdResult.getWinner().getRobot().getNome());
            dto.setThirdTeamNome(thirdResult.getWinner().getTeam().getNome());
        }

        dto.setStatus("CONCLUIDO");
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
