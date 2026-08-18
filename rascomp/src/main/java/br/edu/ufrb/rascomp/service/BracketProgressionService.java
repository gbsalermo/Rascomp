package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BracketProgressionService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final BracketRepository bracketRepository;

    @Transactional
    public void avancarVencedor(Match match, Registration winner) {
        if (winner == null) {
            throw new IllegalArgumentException("Não é possível avançar uma partida sem vencedor.");
        }

        validarVencedorDaPartida(match, winner);

        Match proximaPartida = buscarProximaPartida(match);
        if (proximaPartida == null) {
            finalizarChaveamento(match.getBracket());
            return;
        }

        validarProximaPartidaEditavel(proximaPartida);

        if (match.getOrdem() % 2 != 0) {
            preencherSlotA(proximaPartida, winner);
        } else {
            preencherSlotB(proximaPartida, winner);
        }

        atualizarStatus(proximaPartida);
        matchRepository.save(proximaPartida);
    }

    @Transactional
    public void avancarBye(Match match) {
        if (match.getStatus() != StatusMatch.BYE) {
            return;
        }

        Registration winner = match.getRegistrationA() != null
                ? match.getRegistrationA()
                : match.getRegistrationB();

        if (winner == null) {
            throw new IllegalArgumentException("Partida BYE deve possuir exatamente um participante.");
        }

        match.setStatus(StatusMatch.FINALIZADA);
        matchRepository.save(match);
        avancarVencedor(match, winner);
    }

    private Match buscarProximaPartida(Match match) {
        int proximaRodada = match.getRodada() + 1;
        int proximaOrdem = (match.getOrdem() + 1) / 2;

        return matchRepository
                .findByBracketIdAndRodadaAndOrdem(
                        match.getBracket().getId(),
                        proximaRodada,
                        proximaOrdem)
                .orElse(null);
    }

    private void preencherSlotA(Match proximaPartida, Registration winner) {
        Registration atual = proximaPartida.getRegistrationA();
        if (atual != null && !atual.getId().equals(winner.getId())) {
            throw new IllegalArgumentException("O slot A da próxima partida já está ocupado por outro participante.");
        }
        proximaPartida.setRegistrationA(winner);
    }

    private void preencherSlotB(Match proximaPartida, Registration winner) {
        Registration atual = proximaPartida.getRegistrationB();
        if (atual != null && !atual.getId().equals(winner.getId())) {
            throw new IllegalArgumentException("O slot B da próxima partida já está ocupado por outro participante.");
        }
        proximaPartida.setRegistrationB(winner);
    }

    private void atualizarStatus(Match match) {
        if (match.getRegistrationA() != null && match.getRegistrationB() != null) {
            match.setStatus(StatusMatch.AGENDADA);
        } else {
            match.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
        }
    }

    private void validarVencedorDaPartida(Match match, Registration winner) {
        boolean participanteA = match.getRegistrationA() != null
                && match.getRegistrationA().getId().equals(winner.getId());
        boolean participanteB = match.getRegistrationB() != null
                && match.getRegistrationB().getId().equals(winner.getId());

        if (!participanteA && !participanteB) {
            throw new IllegalArgumentException("O vencedor informado não pertence à partida.");
        }
    }

    private void validarProximaPartidaEditavel(Match match) {
        if (matchResultRepository.existsByMatchId(match.getId())
                || match.getStatus() == StatusMatch.EM_ANDAMENTO
                || match.getStatus() == StatusMatch.FINALIZADA) {
            throw new IllegalArgumentException("A próxima partida já foi iniciada/finalizada e não pode receber alteração de participante.");
        }
    }

    private void finalizarChaveamento(Bracket bracket) {
        Bracket atual = bracketRepository.findById(bracket.getId())
                .orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + bracket.getId()));
        atual.setStatus(StatusBracket.FINALIZADO);
        bracketRepository.save(atual);
    }
}
