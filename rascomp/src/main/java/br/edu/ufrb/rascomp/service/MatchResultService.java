package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.MatchResult;
import br.edu.ufrb.rascomp.model.Registration;
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

    @Transactional
    public MatchResultDTO criar(MatchResultDTO dto) {
        Match match = buscarMatch(dto.getMatchId());
        if (resultRepository.existsByMatchId(match.getId()))
            throw new IllegalArgumentException("A partida já possui resultado.");

        Registration winner = buscarWinnerOpcional(dto.getWinnerRegistrationId());
        validarResultado(match, winner, dto);

        MatchResult result = new MatchResult();
        preencher(result, dto, match, winner);
        match.setStatus(StatusMatch.FINALIZADA);
        matchRepository.save(match);
        return new MatchResultDTO(resultRepository.save(result));
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
        if (resultRepository.existsByMatchIdAndIdNot(match.getId(), id))
            throw new IllegalArgumentException("A partida já possui outro resultado.");

        Registration winner = buscarWinnerOpcional(dto.getWinnerRegistrationId());
        validarResultado(match, winner, dto);
        preencher(result, dto, match, winner);
        match.setStatus(StatusMatch.FINALIZADA);
        matchRepository.save(match);
        return new MatchResultDTO(resultRepository.save(result));
    }

    @Transactional
    public void deletar(Long id) {
        MatchResult result = buscarResult(id);
        Match match = result.getMatch();
        match.setStatus(match.getRegistrationA() == null || match.getRegistrationB() == null
                ? StatusMatch.BYE : StatusMatch.AGENDADA);
        matchRepository.save(match);
        resultRepository.delete(result);
    }

    private void validarResultado(Match match, Registration winner, MatchResultDTO dto) {
        if (!Boolean.TRUE.equals(match.getAtivo()))
            throw new IllegalArgumentException("Não é possível registrar resultado para partida inativa.");

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
