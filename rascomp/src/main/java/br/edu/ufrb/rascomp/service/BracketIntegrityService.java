package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BracketIntegrityService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final RoundSumoRepository roundSumoRepository;

    public void validarEstadoParaGeracao(Competition competition) {
        if (competition.getStatus() != StatusCompetition.INSCRICOES_ENCERRADAS) {
            throw new IllegalArgumentException(
                    "Chaveamento só pode ser gerado ou regenerado com as inscrições encerradas.");
        }
    }

    public void validarRegeneracaoPermitida(List<Bracket> chavesAtuais) {
        chavesAtuais.forEach(this::validarSemAtividadeCompetitiva);
    }

    public void validarSemAtividadeCompetitiva(Bracket bracket) {
        if (possuiAtividadeCompetitiva(bracket)) {
            throw new IllegalArgumentException(
                    "A chave já possui atividade competitiva e não pode ser regenerada ou alterada pelo fluxo comum.");
        }
    }

    public boolean possuiAtividadeCompetitiva(Bracket bracket) {
        if (bracket.getId() == null) return false;

        if (roundSumoRepository.existsByMatchBracketId(bracket.getId())) return true;
        if (matchResultRepository.existsByMatchBracketId(bracket.getId())) return true;

        return matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId())
                .stream()
                .anyMatch(this::partidaIniciadaDeFato);
    }

    private boolean partidaIniciadaDeFato(Match match) {
        if (match.getStatus() == StatusMatch.EM_ANDAMENTO) return true;

        return match.getStatus() == StatusMatch.FINALIZADA
                && match.getRegistrationA() != null
                && match.getRegistrationB() != null;
    }
}
