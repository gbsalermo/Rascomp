package br.edu.ufrb.rascomp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BatalhaSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoDTO;
import br.edu.ufrb.rascomp.dto.RoundSumoItemDTO;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RoundSumo;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.MotivoResultadoRoundSumo;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRoundSumo;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoundSumoService {

    private static final int MAX_PENALIDADES_POR_ROBO = 2;

    private final RoundSumoRepository roundRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final ConfigSumoRepository configSumoRepository;
    private final InspecaoSumoService inspecaoSumoService;
    private final MatchResultService matchResultService;

    @Transactional
    public RoundSumoDTO registrar(RoundSumoDTO dto) {
        Match match = buscarMatch(dto.getMatchId());
        validarPartida(match);

        ConfigSumo config = buscarConfig(match);
        validarInspecoes(match);

        int numeroRound = Math.toIntExact(roundRepository.countByMatchId(match.getId()) + 1);
        validarLimiteDeRounds(match, config, numeroRound);

        Registration winner = buscarWinnerOpcional(dto.getWinnerRegistrationId());
        MotivoResultadoRoundSumo motivo = normalizarMotivo(dto.getMotivoResultado());
        int penalidadesA = normalizarPenalidades(dto.getPenalidadesA(), "A");
        int penalidadesB = normalizarPenalidades(dto.getPenalidadesB(), "B");

        ResultadoRound resolvido = aplicarRegraDePenalidades(
                match,
                winner,
                dto.getStatus(),
                motivo,
                penalidadesA,
                penalidadesB);

        winner = resolvido.winner();
        StatusRoundSumo status = resolvido.status();
        motivo = resolvido.motivo();

        validarResultadoRound(match, winner, status, motivo);

        RoundSumo round = new RoundSumo();
        round.setMatch(match);
        round.setNumeroRound(numeroRound);
        round.setWinner(winner);
        round.setStatus(status);
        round.setMotivoResultado(motivo);
        round.setPenalidadesA(penalidadesA);
        round.setPenalidadesB(penalidadesB);
        round.setObservacao(normalizar(dto.getObservacao()));

        if (match.getStatus() == StatusMatch.AGENDADA) {
            match.setStatus(StatusMatch.EM_ANDAMENTO);
            matchRepository.save(match);
        }

        RoundSumo salvo = roundRepository.save(round);
        apurarResultadoAutomatico(match, config);

        return new RoundSumoDTO(salvo);
    }

    @Transactional
    public List<RoundSumoDTO> registrarBatalha(BatalhaSumoDTO dto) {
        Match match = buscarMatch(dto.getMatchId());
        validarPartida(match);

        List<RoundSumoDTO> registrados = new ArrayList<>();
        for (RoundSumoItemDTO item : dto.getRounds()) {
            RoundSumoDTO round = new RoundSumoDTO();
            round.setMatchId(dto.getMatchId());
            round.setWinnerRegistrationId(item.getWinnerRegistrationId());
            round.setStatus(item.getStatus());
            round.setMotivoResultado(item.getMotivoResultado());
            round.setPenalidadesA(item.getPenalidadesA());
            round.setPenalidadesB(item.getPenalidadesB());
            round.setObservacao(item.getObservacao());
            registrados.add(registrar(round));
        }
        return registrados;
    }

    @Transactional(readOnly = true)
    public RoundSumoDTO buscarPorId(Long id) {
        return new RoundSumoDTO(buscarRound(id));
    }

    @Transactional(readOnly = true)
    public List<RoundSumoDTO> listarPorPartida(Long matchId) {
        buscarMatch(matchId);
        return roundRepository.findByMatchIdOrderByNumeroRoundAsc(matchId)
                .stream()
                .map(RoundSumoDTO::new)
                .toList();
    }

    private void apurarResultadoAutomatico(Match match, ConfigSumo config) {
        int vitoriasA = Math.toIntExact(contarVitorias(match, match.getRegistrationA()));
        int vitoriasB = Math.toIntExact(contarVitorias(match, match.getRegistrationB()));

        if (vitoriasA >= config.getRoundsParaVencer()) {
            matchResultService.criarAutomaticoSumo(
                    match,
                    match.getRegistrationA(),
                    vitoriasA,
                    vitoriasB);
            return;
        }

        if (vitoriasB >= config.getRoundsParaVencer()) {
            matchResultService.criarAutomaticoSumo(
                    match,
                    match.getRegistrationB(),
                    vitoriasA,
                    vitoriasB);
        }
    }

    private ResultadoRound aplicarRegraDePenalidades(
            Match match,
            Registration winner,
            StatusRoundSumo status,
            MotivoResultadoRoundSumo motivo,
            int penalidadesA,
            int penalidadesB) {

        if (status == StatusRoundSumo.ANULADO || status == StatusRoundSumo.CANCELADO) {
            return new ResultadoRound(winner, status, motivo);
        }

        boolean aPerdeuPorPenalidade = penalidadesA == MAX_PENALIDADES_POR_ROBO;
        boolean bPerdeuPorPenalidade = penalidadesB == MAX_PENALIDADES_POR_ROBO;

        if (!aPerdeuPorPenalidade && !bPerdeuPorPenalidade) {
            return new ResultadoRound(winner, status, motivo);
        }

        if (aPerdeuPorPenalidade && bPerdeuPorPenalidade) {
            throw new IllegalArgumentException(
                    "O round é encerrado quando um robô atinge 2 penalidades; os dois lados não podem terminar o mesmo round com 2 penalidades.");
        }

        Registration vencedorAutomatico = aPerdeuPorPenalidade
                ? match.getRegistrationB()
                : match.getRegistrationA();

        return new ResultadoRound(
                vencedorAutomatico,
                StatusRoundSumo.FINALIZADO,
                MotivoResultadoRoundSumo.PENALIDADES);
    }

    private void validarPartida(Match match) {
        if (!Boolean.TRUE.equals(match.getBracket().getAtivo())) {
            throw new IllegalArgumentException("O chaveamento da partida deve estar ativo.");
        }
        if (!Boolean.TRUE.equals(match.getBracket().getAtual())) {
            throw new IllegalArgumentException("Não é possível registrar rounds em uma chave histórica.");
        }
        if (!Boolean.TRUE.equals(match.getAtivo())) {
            throw new IllegalArgumentException("A partida deve estar ativa.");
        }
        if (match.getBracket().getCategory().getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException("Rounds de Sumô só podem ser registrados em categorias SUMO.");
        }
        if (match.getRegistrationA() == null || match.getRegistrationB() == null) {
            throw new IllegalArgumentException("A partida de Sumô deve possuir os dois participantes.");
        }
        if (match.getStatus() == StatusMatch.FINALIZADA || match.getStatus() == StatusMatch.CANCELADA) {
            throw new IllegalArgumentException("Não é possível registrar round em uma partida encerrada.");
        }
        if (match.getStatus() == StatusMatch.BYE || match.getStatus() == StatusMatch.AGUARDANDO_PARTICIPANTES) {
            throw new IllegalArgumentException("A partida ainda não está apta para disputa.");
        }
    }

    private void validarInspecoes(Match match) {
        if (!inspecaoSumoService.estaAptaParaCompetir(match.getRegistrationA().getId())) {
            throw new IllegalArgumentException("Participante A não está apto na inspeção do Sumô.");
        }
        if (!inspecaoSumoService.estaAptaParaCompetir(match.getRegistrationB().getId())) {
            throw new IllegalArgumentException("Participante B não está apto na inspeção do Sumô.");
        }
    }

    private void validarLimiteDeRounds(Match match, ConfigSumo config, int numeroRound) {
        if (numeroRound <= config.getNumeroRounds()) {
            return;
        }

        boolean podeDesempate = Boolean.TRUE.equals(config.getPermiteRoundDesempate())
                && numeroRound == config.getNumeroRounds() + 1;

        if (!podeDesempate) {
            throw new IllegalArgumentException("Limite de rounds atingido para esta categoria.");
        }

        long vitoriasA = contarVitorias(match, match.getRegistrationA());
        long vitoriasB = contarVitorias(match, match.getRegistrationB());

        if (vitoriasA >= config.getRoundsParaVencer() || vitoriasB >= config.getRoundsParaVencer()) {
            throw new IllegalArgumentException("A partida já possui vencedor e não necessita round de desempate.");
        }
    }

    private void validarResultadoRound(
            Match match,
            Registration winner,
            StatusRoundSumo status,
            MotivoResultadoRoundSumo motivo) {

        if (status == StatusRoundSumo.FINALIZADO) {
            if (winner == null) {
                throw new IllegalArgumentException("Round finalizado deve possuir vencedor.");
            }
            boolean participanteA = match.getRegistrationA().getId().equals(winner.getId());
            boolean participanteB = match.getRegistrationB().getId().equals(winner.getId());
            if (!participanteA && !participanteB) {
                throw new IllegalArgumentException("O vencedor do round deve ser participante da partida.");
            }
            return;
        }

        if (motivo == MotivoResultadoRoundSumo.SUICIDIO_WO
                || motivo == MotivoResultadoRoundSumo.PENALIDADES) {
            throw new IllegalArgumentException(
                    "Suicídio/WO ou derrota por penalidades deve encerrar o round com vitória do adversário.");
        }

        if (winner != null) {
            throw new IllegalArgumentException("Round empatado, anulado ou cancelado não deve possuir vencedor.");
        }
    }

    private long contarVitorias(Match match, Registration participante) {
        return roundRepository.findByMatchIdOrderByNumeroRoundAsc(match.getId())
                .stream()
                .filter(round -> round.getStatus() == StatusRoundSumo.FINALIZADO)
                .filter(round -> round.getWinner() != null && round.getWinner().getId().equals(participante.getId()))
                .count();
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

    private RoundSumo buscarRound(Long id) {
        return roundRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Round de Sumô não encontrado: " + id));
    }

    private Registration buscarWinnerOpcional(Long id) {
        if (id == null) return null;
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição vencedora não encontrada: " + id));
    }

    private MotivoResultadoRoundSumo normalizarMotivo(MotivoResultadoRoundSumo motivo) {
        return motivo == null ? MotivoResultadoRoundSumo.DISPUTA : motivo;
    }

    private int normalizarPenalidades(Integer quantidade, String lado) {
        int valor = quantidade == null ? 0 : quantidade;
        if (valor < 0 || valor > MAX_PENALIDADES_POR_ROBO) {
            throw new IllegalArgumentException(
                    "Penalidades do robô " + lado + " devem estar entre 0 e " + MAX_PENALIDADES_POR_ROBO + ".");
        }
        return valor;
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record ResultadoRound(
            Registration winner,
            StatusRoundSumo status,
            MotivoResultadoRoundSumo motivo) {
    }
}
