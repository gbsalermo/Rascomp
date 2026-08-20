package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchResultService {
    private final MatchResultRepository resultRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final BracketProgressionService bracketProgressionService;

    @Transactional
    public MatchResultDTO criar(MatchResultDTO dto) {
        Match match = buscarMatch(dto.getMatchId());
        validarOperacaoManualPermitida(match);

        if (resultRepository.existsByMatchId(match.getId()))
            throw new IllegalArgumentException("A partida já possui resultado.");

        Registration winner = buscarWinnerOpcional(dto.getWinnerRegistrationId());
        validarResultado(match, winner, dto);

        return salvarResultado(
                match,
                winner,
                dto.getPontosA(),
                dto.getPontosB(),
                dto.getObservacao());
    }

    @Transactional
    public MatchResultDTO criarAutomaticoSumo(
            Match match,
            Registration winner,
            int vitoriasA,
            int vitoriasB) {

        if (match.getBracket().getCategory().getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException("Resultado automático de Sumô só pode ser usado em categoria SUMO.");
        }

        if (resultRepository.existsByMatchId(match.getId())) {
            return new MatchResultDTO(resultRepository.findByMatchId(match.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Resultado não encontrado para a partida: " + match.getId())));
        }

        if (winner == null) {
            throw new IllegalArgumentException("O resultado automático do Sumô exige vencedor.");
        }

        return salvarResultado(
                match,
                winner,
                vitoriasA,
                vitoriasB,
                "Resultado consolidado automaticamente pelos rounds do Sumô.");
    }

    @Transactional(readOnly = true)
    public List<MatchResultDTO> listarTodos() {
        return resultRepository.findAllByOrderByIdAsc()
                .stream()
                .map(MatchResultDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResultDTO> listarPorChaveamento(Long bracketId) {
        return resultRepository.findByMatchBracketIdOrderByIdAsc(bracketId)
                .stream()
                .map(MatchResultDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResultDTO> listarPorCompeticao(Long competitionId) {
        return resultRepository
                .findByMatchBracketCompetitionIdOrderByIdAsc(competitionId)
                .stream()
                .map(MatchResultDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResultDTO buscarPorId(Long id) {
        return new MatchResultDTO(buscarResult(id));
    }

    @Transactional(readOnly = true)
    public MatchResultDTO buscarPorPartida(Long matchId) {
        buscarMatch(matchId);
        MatchResult result = resultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Resultado não encontrado para a partida: " + matchId));
        return new MatchResultDTO(result);
    }

    @Transactional
    public MatchResultDTO atualizar(Long id, MatchResultDTO dto) {
        MatchResult result = buscarResult(id);
        Match match = buscarMatch(dto.getMatchId());
        validarOperacaoManualPermitida(match);

        if (resultRepository.existsByMatchIdAndIdNot(match.getId(), id))
            throw new IllegalArgumentException("A partida já possui outro resultado.");

        Registration winner = buscarWinnerOpcional(dto.getWinnerRegistrationId());
        validarResultado(match, winner, dto);
        preencher(result, dto, match, winner);
        MatchResult salvo = resultRepository.save(result);

        match.setStatus(StatusMatch.FINALIZADA);
        matchRepository.save(match);

        if (winner != null) {
            bracketProgressionService.avancarVencedor(match, winner);
        }

        return new MatchResultDTO(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        MatchResult result = buscarResult(id);
        Match match = result.getMatch();
        validarOperacaoManualPermitida(match);

        match.setStatus(match.getRegistrationA() == null || match.getRegistrationB() == null
                ? StatusMatch.BYE : StatusMatch.AGENDADA);
        matchRepository.save(match);
        resultRepository.delete(result);
    }

    private void validarOperacaoManualPermitida(Match match) {
        Modalidade modalidade = match.getBracket().getCategory().getModalidade();

        if (modalidade == Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException(
                    "FOLLOW_LINE é definido pelo ranking de tempos e não utiliza partidas nem MatchResult.");
        }

        if (modalidade == Modalidade.SUMO) {
            throw new IllegalArgumentException(
                    "O resultado de uma partida de Sumô é calculado automaticamente pelos rounds.");
        }
    }

    private MatchResultDTO salvarResultado(
            Match match,
            Registration winner,
            Integer pontosA,
            Integer pontosB,
            String observacao) {

        MatchResult result = new MatchResult();
        result.setMatch(match);
        result.setWinner(winner);
        result.setPontosA(pontosA);
        result.setPontosB(pontosB);
        result.setObservacao(observacao == null || observacao.isBlank() ? null : observacao.trim());

        MatchResult salvo = resultRepository.save(result);

        match.setStatus(StatusMatch.FINALIZADA);
        matchRepository.save(match);

        if (winner != null) {
            bracketProgressionService.avancarVencedor(match, winner);
        }

        return new MatchResultDTO(salvo);
    }

    private void validarResultado(Match match, Registration winner, MatchResultDTO dto) {
        if (!Boolean.TRUE.equals(match.getAtivo()))
            throw new IllegalArgumentException("Não é possível registrar resultado para partida inativa.");

        if (match.getStatus() == StatusMatch.AGUARDANDO_PARTICIPANTES)
            throw new IllegalArgumentException("A partida ainda aguarda participantes.");

        if (match.getStatus() == StatusMatch.BYE)
            throw new IllegalArgumentException("Partida BYE avança automaticamente e não recebe resultado manual.");

        if (winner != null) {
            boolean participanteA = match.getRegistrationA() != null
                    && match.getRegistrationA().getId().equals(winner.getId());
            boolean participanteB = match.getRegistrationB() != null
                    && match.getRegistrationB().getId().equals(winner.getId());
            if (!participanteA && !participanteB)
                throw new IllegalArgumentException("O vencedor deve ser um participante da partida.");
        }

        if (dto.getPontosA().equals(dto.getPontosB()) && winner != null)
            throw new IllegalArgumentException("Não pode haver vencedor quando a pontuação está empatada.");
        if (!dto.getPontosA().equals(dto.getPontosB()) && winner == null)
            throw new IllegalArgumentException("Informe o vencedor quando a pontuação não estiver empatada.");
    }

    private Match buscarMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + id));
    }

    private MatchResult buscarResult(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resultado não encontrado: " + id));
    }

    private Registration buscarWinnerOpcional(Long id) {
        if (id == null) return null;
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição vencedora não encontrada: " + id));
    }

    private void preencher(MatchResult entity, MatchResultDTO dto, Match match, Registration winner) {
        entity.setMatch(match);
        entity.setWinner(winner);
        entity.setPontosA(dto.getPontosA());
        entity.setPontosB(dto.getPontosB());
        entity.setObservacao(dto.getObservacao() == null || dto.getObservacao().isBlank()
                ? null : dto.getObservacao().trim());
    }
}
