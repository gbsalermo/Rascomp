package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.MatchJudgeDecisionDTO;
import br.edu.ufrb.rascomp.model.CompetitionJudge;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchJudgeDecision;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.repository.CompetitionJudgeRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.MatchJudgeDecisionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchJudgeDecisionService {

    private final MatchJudgeDecisionRepository decisionRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final CompetitionJudgeRepository judgeRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final RoundSumoRepository roundRepository;
    private final MatchResultService matchResultService;

    @Transactional
    public MatchJudgeDecisionDTO decidir(MatchJudgeDecisionDTO dto) {
        Match match = buscarMatch(dto.getMatchId());
        validarPartida(match);

        if (decisionRepository.existsByMatchId(match.getId())) {
            throw new IllegalArgumentException("A partida já possui decisão de juiz registrada.");
        }

        ConfigSumo config = buscarConfig(match);
        validarRoundsEsgotados(match, config);

        Registration winner = buscarRegistration(dto.getWinnerRegistrationId());
        validarWinner(match, winner);

        CompetitionJudge judge = buscarJudge(dto.getJudgeId());
        validarJudge(match, judge);

        String justificativa = normalizarJustificativa(dto.getJustificativa());
        int vitoriasA = contarVitorias(match, match.getRegistrationA());
        int vitoriasB = contarVitorias(match, match.getRegistrationB());

        MatchJudgeDecision decision = new MatchJudgeDecision();
        decision.setMatch(match);
        decision.setWinner(winner);
        decision.setJudge(judge);
        decision.setJustificativa(justificativa);

        MatchJudgeDecision salva = decisionRepository.save(decision);
        matchResultService.criarPorDecisaoJuiz(
                match,
                winner,
                vitoriasA,
                vitoriasB,
                judge.getNome(),
                justificativa);

        return new MatchJudgeDecisionDTO(salva);
    }

    @Transactional(readOnly = true)
    public MatchJudgeDecisionDTO buscarPorPartida(Long matchId) {
        buscarMatch(matchId);
        return decisionRepository.findByMatchId(matchId)
                .map(MatchJudgeDecisionDTO::new)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhuma decisão de juiz encontrada para a partida: " + matchId));
    }

    private void validarPartida(Match match) {
        if (!Boolean.TRUE.equals(match.getAtivo())) {
            throw new IllegalArgumentException("A partida deve estar ativa.");
        }
        if (!Boolean.TRUE.equals(match.getBracket().getAtivo()) || !Boolean.TRUE.equals(match.getBracket().getAtual())) {
            throw new IllegalArgumentException("Decisão de juiz só pode ser registrada na chave atual e ativa.");
        }
        if (match.getBracket().getCategory().getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException("Decisão de juiz deste fluxo só pode ser usada em partidas de Sumô.");
        }
        if (match.getRegistrationA() == null || match.getRegistrationB() == null) {
            throw new IllegalArgumentException("A partida deve possuir os dois participantes.");
        }
        if (match.getStatus() == StatusMatch.FINALIZADA || match.getStatus() == StatusMatch.CANCELADA
                || match.getStatus() == StatusMatch.BYE || match.getStatus() == StatusMatch.AGUARDANDO_PARTICIPANTES) {
            throw new IllegalArgumentException("A partida não está apta para decisão de juiz.");
        }
    }

    private void validarRoundsEsgotados(Match match, ConfigSumo config) {
        int maxExtras = Boolean.TRUE.equals(config.getPermiteRoundDesempate())
                ? (config.getMaxRoundsExtras() == null ? 0 : config.getMaxRoundsExtras())
                : 0;
        int minimoAntesDaDecisao = config.getNumeroRounds() + maxExtras;
        long registrados = roundRepository.countByMatchId(match.getId());

        if (registrados < minimoAntesDaDecisao) {
            throw new IllegalArgumentException(
                    "Decisão de juiz só pode ser usada após esgotar os rounds regulares e extras disponíveis.");
        }

        int vitoriasA = contarVitorias(match, match.getRegistrationA());
        int vitoriasB = contarVitorias(match, match.getRegistrationB());
        if (vitoriasA >= config.getRoundsParaVencer() || vitoriasB >= config.getRoundsParaVencer()) {
            throw new IllegalArgumentException("A partida já possui vencedor pelos rounds disputados.");
        }
    }

    private int contarVitorias(Match match, Registration registration) {
        return Math.toIntExact(roundRepository.findByMatchIdOrderByNumeroRoundAsc(match.getId())
                .stream()
                .filter(round -> round.getStatus() == StatusRoundSumo.FINALIZADO)
                .filter(round -> round.getWinner() != null
                        && round.getWinner().getId().equals(registration.getId()))
                .count());
    }

    private void validarWinner(Match match, Registration winner) {
        boolean participanteA = match.getRegistrationA().getId().equals(winner.getId());
        boolean participanteB = match.getRegistrationB().getId().equals(winner.getId());
        if (!participanteA && !participanteB) {
            throw new IllegalArgumentException("O vencedor decidido deve participar da partida.");
        }
    }

    private void validarJudge(Match match, CompetitionJudge judge) {
        if (!Boolean.TRUE.equals(judge.getAtivo())) {
            throw new IllegalArgumentException("O juiz selecionado está inativo.");
        }
        if (!judge.getCompetition().getId().equals(match.getBracket().getCompetition().getId())) {
            throw new IllegalArgumentException("O juiz selecionado não pertence à competição da partida.");
        }
    }

    private ConfigSumo buscarConfig(Match match) {
        Long categoryId = match.getBracket().getCategory().getId();
        return configSumoRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Sumô não encontrada para a categoria: " + categoryId));
    }

    private Match buscarMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + id));
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private CompetitionJudge buscarJudge(Long id) {
        return judgeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Juiz não encontrado: " + id));
    }

    private String normalizarJustificativa(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Justificativa da decisão do juiz é obrigatória.");
        }
        String normalized = valor.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Justificativa deve ter no máximo 500 caracteres.");
        }
        return normalized;
    }
}
