package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final BracketRepository bracketRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public MatchDTO criar(MatchDTO dto) {
        Bracket bracket = buscarBracket(dto.getBracketId());
        Registration a = buscarRegistrationOpcional(dto.getRegistrationAId());
        Registration b = buscarRegistrationOpcional(dto.getRegistrationBId());
        validarParticipantes(bracket, a, b);
        validarDuplicidade(dto, null);

        Match match = new Match();
        preencher(match, dto, bracket, a, b);
        match.setStatus(dto.getStatus() != null ? dto.getStatus() : definirStatusInicial(a, b));
        match.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new MatchDTO(matchRepository.save(match));
    }

    @Transactional(readOnly = true)
    public List<MatchDTO> listarPorChaveamento(Long bracketId) {
        buscarBracket(bracketId);
        return matchRepository.findByBracketIdOrderByRodadaAscOrdemAsc(bracketId)
                .stream().map(MatchDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public MatchDTO buscarPorId(Long id) {
        return new MatchDTO(buscarMatch(id));
    }

    @Transactional
    public MatchDTO atualizar(Long id, MatchDTO dto) {
        Match match = buscarMatch(id);
        Bracket bracket = buscarBracket(dto.getBracketId());
        Registration a = buscarRegistrationOpcional(dto.getRegistrationAId());
        Registration b = buscarRegistrationOpcional(dto.getRegistrationBId());
        validarParticipantes(bracket, a, b);
        validarDuplicidade(dto, id);
        preencher(match, dto, bracket, a, b);
        if (dto.getStatus() != null) match.setStatus(dto.getStatus());
        if (dto.getAtivo() != null) match.setAtivo(dto.getAtivo());
        return new MatchDTO(matchRepository.save(match));
    }

    @Transactional
    public void deletar(Long id) {
        Match match = buscarMatch(id);
        match.setAtivo(false);
        match.setStatus(StatusMatch.CANCELADA);
        matchRepository.save(match);
    }

    @Transactional
    public MatchDTO reativar(Long id) {
        Match match = buscarMatch(id);
        match.setAtivo(true);
        match.setStatus(definirStatusInicial(match.getRegistrationA(), match.getRegistrationB()));
        return new MatchDTO(matchRepository.save(match));
    }

    private void validarParticipantes(Bracket bracket, Registration a, Registration b) {
        if (!Boolean.TRUE.equals(bracket.getAtivo())) throw new IllegalArgumentException("Chaveamento inativo.");
        if (a == null && b == null) throw new IllegalArgumentException("A partida deve possuir pelo menos um participante.");
        if (a != null) validarRegistrationDoBracket(bracket, a);
        if (b != null) validarRegistrationDoBracket(bracket, b);
        if (a != null && b != null && a.getId().equals(b.getId()))
            throw new IllegalArgumentException("Os participantes da partida devem ser diferentes.");
    }

    private void validarRegistrationDoBracket(Bracket bracket, Registration registration) {
        if (!Boolean.TRUE.equals(registration.getAtivo()) || registration.getStatus() != StatusRegistration.APROVADA)
            throw new IllegalArgumentException("Participante deve possuir inscrição ativa e aprovada.");
        if (!registration.getCompetition().getId().equals(bracket.getCompetition().getId())
                || !registration.getCategory().getId().equals(bracket.getCategory().getId()))
            throw new IllegalArgumentException("A inscrição não pertence à competição e categoria do chaveamento.");
    }

    private StatusMatch definirStatusInicial(Registration a, Registration b) {
        return a == null || b == null ? StatusMatch.BYE : StatusMatch.AGENDADA;
    }

    private void validarDuplicidade(MatchDTO dto, Long id) {
        boolean existe = id == null
                ? matchRepository.existsByBracketIdAndRodadaAndOrdem(dto.getBracketId(), dto.getRodada(), dto.getOrdem())
                : matchRepository.existsByBracketIdAndRodadaAndOrdemAndIdNot(dto.getBracketId(), dto.getRodada(), dto.getOrdem(), id);
        if (existe) throw new IllegalArgumentException("Já existe uma partida nessa rodada e ordem.");
    }

    private Match buscarMatch(Long id) { return matchRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + id)); }
    private Bracket buscarBracket(Long id) { return bracketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + id)); }
    private Registration buscarRegistrationOpcional(Long id) {
        if (id == null) return null;
        return registrationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private void preencher(Match entity, MatchDTO dto, Bracket bracket, Registration a, Registration b) {
        entity.setBracket(bracket);
        entity.setRodada(dto.getRodada());
        entity.setOrdem(dto.getOrdem());
        entity.setRegistrationA(a);
        entity.setRegistrationB(b);
        entity.setDataHora(dto.getDataHora());
    }
}
