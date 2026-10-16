package br.edu.ufrb.rascomp.service;

import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.TipoPartidaSumo;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BracketProgressionService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final RoundSumoRepository roundSumoRepository;
    private final BracketRepository bracketRepository;

    @Transactional
    public void avancarVencedor(Match match, Registration winner) {
        if (winner == null) {
            throw new IllegalArgumentException("Não é possível avançar uma partida sem vencedor.");
        }

        validarVencedorDaPartida(match, winner);

        if ("DOUBLE_ELIMINATION".equals(match.getBracket().getBracketFormat())) {
            avancarDuplaEliminacao(match, winner);
            resolverAvancosAutomaticos(match.getBracket().getId());
            return;
        }
        alimentarTerceiroLugarSeSemifinal(match, winner);

        Match proximaPartida = buscarProximaPartida(match);
        if (proximaPartida == null) {
            tentarFinalizarChaveamento(match.getBracket());
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

    private void avancarDuplaEliminacao(Match match, Registration winner) {
        Registration loser = oponente(match, winner);
        String section = match.getBracketSection();
        if (loser == null && match.getStatus() != StatusMatch.BYE) {
            throw new IllegalArgumentException("A partida precisa de dois participantes para registrar uma derrota.");
        }
        if ("GRAND_FINAL".equals(section)) {
            if (loser == null) throw new IllegalArgumentException("Final geral não permite BYE.");
            if (match.getRegistrationA().getId().equals(winner.getId())) {
                finalizarDuplaEliminacao(match.getBracket());
            } else {
                Match reset = match.getWinnerNextMatch();
                if (reset == null || !"GRAND_FINAL_RESET".equals(reset.getBracketSection())) {
                    throw new IllegalStateException("Final de reset não configurada.");
                }
                enviarParaSlot(reset, "A", winner);
                enviarParaSlot(reset, "B", loser);
            }
            return;
        }
        if ("GRAND_FINAL_RESET".equals(section)) {
            finalizarDuplaEliminacao(match.getBracket());
            return;
        }
        if (match.getWinnerNextMatch() != null) {
            enviarParaSlot(match.getWinnerNextMatch(), match.getWinnerNextSlot(), winner);
        }
        if (loser != null && match.getLoserNextMatch() != null) {
            enviarParaSlot(match.getLoserNextMatch(), match.getLoserNextSlot(), loser);
        }
    }

    private void enviarParaSlot(Match target, String slot, Registration registration) {
        validarProximaPartidaEditavel(target);
        if ("A".equals(slot)) preencherSlotA(target, registration);
        else if ("B".equals(slot)) preencherSlotB(target, registration);
        else throw new IllegalArgumentException("Destino de partida sem slot configurado.");
        atualizarStatus(target);
        matchRepository.save(target);
    }

    private void finalizarDuplaEliminacao(Bracket bracket) {
        Bracket atual = buscarBracket(bracket.getId());
        atual.setStatus(StatusBracket.FINALIZADO);
        bracketRepository.save(atual);
    }

    /**
     * Só resolve um BYE quando TODAS as duas origens da partida estão decididas.
     * Isso impede eliminar cedo um perdedor que ainda chegará da Winners.
     * Partidas com zero participantes são caminhos vazios, nunca vitórias ou derrotas.
     */
    @Transactional
    public void resolverAvancosAutomaticos(Long bracketId) {
        List<Match> all = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId);
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            for (Match target : all) {
                if ("GRAND_FINAL_RESET".equals(target.getBracketSection())
                        || target.getStatus() != StatusMatch.AGUARDANDO_PARTICIPANTES) continue;

                boolean hasA = false, hasB = false, readyA = false, readyB = false;
                for (Match source : all) {
                    if (sameTarget(source.getWinnerNextMatch(), target)) {
                        if ("A".equals(source.getWinnerNextSlot())) {
                            hasA = true;
                            readyA = source.getStatus() == StatusMatch.FINALIZADA;
                        } else if ("B".equals(source.getWinnerNextSlot())) {
                            hasB = true;
                            readyB = source.getStatus() == StatusMatch.FINALIZADA;
                        }
                    }
                    if (sameTarget(source.getLoserNextMatch(), target)) {
                        if ("A".equals(source.getLoserNextSlot())) {
                            hasA = true;
                            readyA = source.getStatus() == StatusMatch.FINALIZADA;
                        } else if ("B".equals(source.getLoserNextSlot())) {
                            hasB = true;
                            readyB = source.getStatus() == StatusMatch.FINALIZADA;
                        }
                    }
                }
                if (!hasA || !hasB || !readyA || !readyB) continue;

                Registration a = target.getRegistrationA(), b = target.getRegistrationB();
                if (a != null && b != null) {
                    target.setStatus(StatusMatch.AGENDADA);
                } else {
                    target.setStatus(StatusMatch.FINALIZADA);
                    matchRepository.save(target);
                    if (a != null || b != null) {
                        avancarDuplaEliminacao(target, a != null ? a : b);
                    }
                    // Empty paths carry no winner and automatically unblock
                    // the next round through this fixed-point traversal.
                }
                matchRepository.save(target);
                changed = true;
            }
            if (++guard > all.size() + 1) {
                throw new IllegalStateException("Progressão automática não convergiu.");
            }
        } while (changed);
    }

    private boolean sameTarget(Match candidate, Match target) {
        return candidate != null && candidate.getId().equals(target.getId());
    }

    @Transactional
    public void corrigirVencedor(
            Match match,
            Registration vencedorAnterior,
            Registration novoVencedor) {

        if (vencedorAnterior != null) validarVencedorDaPartida(match, vencedorAnterior);
        if (novoVencedor != null) validarVencedorDaPartida(match, novoVencedor);

        if (mesmaInscricao(vencedorAnterior, novoVencedor)) return;
        if ("DOUBLE_ELIMINATION".equals(match.getBracket().getBracketFormat())) {
            corrigirDuplaEliminacao(match, novoVencedor);
            return;
        }

        Match proximaPartida = buscarProximaPartida(match);
        if (proximaPartida == null) {
            if (novoVencedor == null) reabrirChaveamento(match.getBracket());
            else tentarFinalizarChaveamento(match.getBracket());
            return;
        }

        validarProximaPartidaEditavel(proximaPartida);
        corrigirTerceiroLugarSeSemifinal(match, vencedorAnterior, novoVencedor);

        if (match.getOrdem() % 2 != 0) {
            corrigirSlotA(proximaPartida, vencedorAnterior, novoVencedor);
        } else {
            corrigirSlotB(proximaPartida, vencedorAnterior, novoVencedor);
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

    @Transactional
    public void marcarChaveEmAndamento(Bracket bracket) {
        Bracket atual = buscarBracket(bracket.getId());
        if (atual.getStatus() == StatusBracket.GERADO) {
            atual.setStatus(StatusBracket.EM_ANDAMENTO);
            bracketRepository.save(atual);
        }
    }

    private Match buscarProximaPartida(Match match) {
        if (match.getTipoPartida() == TipoPartidaSumo.TERCEIRO_LUGAR) {
            return null;
        }

        int proximaRodada = match.getRodada() + 1;
        int proximaOrdem = (match.getOrdem() + 1) / 2;

        return matchRepository
                .findByBracketIdAndRodadaAndOrdem(
                        match.getBracket().getId(),
                        proximaRodada,
                        proximaOrdem)
                .orElse(null);
    }

    /**
     * Reconstitui os destinos da partida corrigida. Descendentes que já possuem
     * rounds/resultados são imutáveis pelo fluxo excepcional: alterações nesses
     * casos exigem uma intervenção auditada específica, sem apagar disputas.
     */
    private void corrigirDuplaEliminacao(Match source, Registration correctedWinner) {
        List<Match> all = matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(source.getBracket().getId());
        Set<Long> affected = new HashSet<>();
        ArrayDeque<Match> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty()) {
            Match current = queue.removeFirst();
            for (Match target : List.of(current.getWinnerNextMatch() == null
                    ? current : current.getWinnerNextMatch(),
                    current.getLoserNextMatch() == null ? current : current.getLoserNextMatch())) {
                if (target.getId().equals(source.getId()) || !affected.add(target.getId())) continue;
                queue.add(target);
            }
        }

        for (Match downstream : all) {
            if (!affected.contains(downstream.getId())) continue;
            if (matchResultRepository.existsByMatchId(downstream.getId())
                    || roundSumoRepository.existsByMatchId(downstream.getId())
                    || downstream.getStatus() == StatusMatch.EM_ANDAMENTO
                    || downstream.getStatus() == StatusMatch.CANCELADA) {
                throw new IllegalArgumentException(
                        "Uma partida dependente já possui disputa, resultado ou bloqueio. Não é seguro corrigir o vencedor.");
            }
        }

        for (Match downstream : all) {
            if (!affected.contains(downstream.getId())) continue;
            downstream.setRegistrationA(null);
            downstream.setRegistrationB(null);
            downstream.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
            downstream.setStatusConvocacao(StatusConvocacaoPartida.NAO_CONVOCADA);
            matchRepository.save(downstream);
        }

        reabrirChaveamento(source.getBracket());
        if (correctedWinner == null) {
            source.setStatus(StatusMatch.AGENDADA);
            matchRepository.save(source);
        }

        // Reaplica exclusivamente contribuições de origens fora da região
        // afetada (inclusive a nova decisão da partida de origem).
        for (Match upstream : all) {
            if (affected.contains(upstream.getId()) || upstream.getStatus() != StatusMatch.FINALIZADA) continue;
            Registration winner;
            if (upstream.getId().equals(source.getId())) {
                winner = correctedWinner;
            } else {
                var result = matchResultRepository.findByMatchId(upstream.getId()).orElse(null);
                winner = result != null ? result.getWinner()
                        : upstream.getRegistrationA() == null ? upstream.getRegistrationB()
                        : upstream.getRegistrationB() == null ? upstream.getRegistrationA() : null;
            }
            if (winner == null) continue;
            Registration loser = oponente(upstream, winner);
            if ("GRAND_FINAL".equals(upstream.getBracketSection())) {
                if (upstream.getRegistrationA().getId().equals(winner.getId())) {
                    finalizarDuplaEliminacao(upstream.getBracket());
                    continue;
                }
            }
            if (upstream.getWinnerNextMatch() != null
                    && affected.contains(upstream.getWinnerNextMatch().getId())) {
                enviarParaSlot(upstream.getWinnerNextMatch(), upstream.getWinnerNextSlot(), winner);
            }
            if (loser != null && upstream.getLoserNextMatch() != null
                    && affected.contains(upstream.getLoserNextMatch().getId())) {
                enviarParaSlot(upstream.getLoserNextMatch(), upstream.getLoserNextSlot(), loser);
            }
        }
        resolverAvancosAutomaticos(source.getBracket().getId());
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

    private void corrigirSlotA(
            Match proximaPartida,
            Registration vencedorAnterior,
            Registration novoVencedor) {
        validarSlotCorrigivel(proximaPartida.getRegistrationA(), vencedorAnterior, novoVencedor, "A");
        proximaPartida.setRegistrationA(novoVencedor);
    }

    private void corrigirSlotB(
            Match proximaPartida,
            Registration vencedorAnterior,
            Registration novoVencedor) {
        validarSlotCorrigivel(proximaPartida.getRegistrationB(), vencedorAnterior, novoVencedor, "B");
        proximaPartida.setRegistrationB(novoVencedor);
    }

    private void validarSlotCorrigivel(
            Registration atual,
            Registration vencedorAnterior,
            Registration novoVencedor,
            String slot) {

        if (atual == null) return;
        if (mesmaInscricao(atual, vencedorAnterior)) return;
        if (mesmaInscricao(atual, novoVencedor)) return;

        throw new IllegalArgumentException(
                "O slot " + slot + " da próxima partida contém outro participante e não pode ser corrigido automaticamente.");
    }

    private boolean mesmaInscricao(Registration a, Registration b) {
        if (a == null || b == null) return a == b;
        return a.getId().equals(b.getId());
    }

    private void atualizarStatus(Match match) {
        if (match.getRegistrationA() != null && match.getRegistrationB() != null) {
            match.setStatus(StatusMatch.AGENDADA);
        } else {
            match.setStatus(StatusMatch.AGUARDANDO_PARTICIPANTES);
            match.setStatusConvocacao(StatusConvocacaoPartida.NAO_CONVOCADA);
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
                || roundSumoRepository.existsByMatchId(match.getId())
                || match.getStatus() == StatusMatch.EM_ANDAMENTO
                || match.getStatus() == StatusMatch.FINALIZADA
                || match.getStatus() == StatusMatch.CANCELADA) {
            throw new IllegalArgumentException(
                    "A próxima partida já foi iniciada/finalizada e não pode receber correção de participante.");
        }
    }

    private void tentarFinalizarChaveamento(Bracket bracket) {
        List<Match> matches = matchRepository
                .findByBracketIdOrderByRodadaAscOrdemAsc(bracket.getId());

        Match finalPrincipal = matches.stream()
                .filter(item -> item.getTipoPartida() != TipoPartidaSumo.TERCEIRO_LUGAR)
                .max(Comparator.comparing(Match::getRodada)
                        .thenComparing(item -> -item.getOrdem()))
                .orElse(null);

        if (finalPrincipal == null
                || !matchResultRepository.existsByMatchId(finalPrincipal.getId())) {
            return;
        }

        Match terceiroLugar = matches.stream()
                .filter(item -> item.getTipoPartida() == TipoPartidaSumo.TERCEIRO_LUGAR)
                .findFirst()
                .orElse(null);

        if (terceiroLugar != null
                && terceiroLugar.getRegistrationA() != null
                && terceiroLugar.getRegistrationB() != null
                && !matchResultRepository.existsByMatchId(terceiroLugar.getId())) {
            return;
        }

        Bracket atual = buscarBracket(bracket.getId());
        atual.setStatus(StatusBracket.FINALIZADO);
        bracketRepository.save(atual);
    }

    private void alimentarTerceiroLugarSeSemifinal(Match match, Registration winner) {
        Match terceiroLugar = buscarTerceiroLugar(match);
        if (terceiroLugar == null || !ehSemifinal(match, terceiroLugar)) return;
        if (match.getRegistrationA() == null || match.getRegistrationB() == null) return;

        Registration loser = oponente(match, winner);
        if (loser == null) return;

        validarProximaPartidaEditavel(terceiroLugar);
        if (match.getOrdem() == 1) {
            preencherSlotA(terceiroLugar, loser);
        } else {
            preencherSlotB(terceiroLugar, loser);
        }
        atualizarStatus(terceiroLugar);
        matchRepository.save(terceiroLugar);
    }

    private void corrigirTerceiroLugarSeSemifinal(
            Match match,
            Registration vencedorAnterior,
            Registration novoVencedor) {

        Match terceiroLugar = buscarTerceiroLugar(match);
        if (terceiroLugar == null || !ehSemifinal(match, terceiroLugar)) return;
        if (match.getRegistrationA() == null || match.getRegistrationB() == null) return;

        validarProximaPartidaEditavel(terceiroLugar);

        Registration perdedorAnterior = oponente(match, vencedorAnterior);
        Registration novoPerdedor = oponente(match, novoVencedor);

        if (match.getOrdem() == 1) {
            corrigirSlotA(terceiroLugar, perdedorAnterior, novoPerdedor);
        } else {
            corrigirSlotB(terceiroLugar, perdedorAnterior, novoPerdedor);
        }

        atualizarStatus(terceiroLugar);
        matchRepository.save(terceiroLugar);
    }

    private Match buscarTerceiroLugar(Match match) {
        if (match.getTipoPartida() == TipoPartidaSumo.TERCEIRO_LUGAR) return null;

        return matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(match.getBracket().getId())
                .stream()
                .filter(item -> item.getTipoPartida() == TipoPartidaSumo.TERCEIRO_LUGAR)
                .findFirst()
                .orElse(null);
    }

    private boolean ehSemifinal(Match match, Match terceiroLugar) {
        return match.getTipoPartida() != TipoPartidaSumo.TERCEIRO_LUGAR
                && match.getRodada() + 1 == terceiroLugar.getRodada()
                && (match.getOrdem() == 1 || match.getOrdem() == 2);
    }

    private Registration oponente(Match match, Registration participante) {
        if (participante == null) return null;
        if (match.getRegistrationA() != null
                && match.getRegistrationA().getId().equals(participante.getId())) {
            return match.getRegistrationB();
        }
        if (match.getRegistrationB() != null
                && match.getRegistrationB().getId().equals(participante.getId())) {
            return match.getRegistrationA();
        }
        return null;
    }

    private void reabrirChaveamento(Bracket bracket) {
        Bracket atual = buscarBracket(bracket.getId());
        if (atual.getStatus() == StatusBracket.FINALIZADO) {
            atual.setStatus(StatusBracket.EM_ANDAMENTO);
            bracketRepository.save(atual);
        }
    }

    private Bracket buscarBracket(Long id) {
        return bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + id));
    }
}
