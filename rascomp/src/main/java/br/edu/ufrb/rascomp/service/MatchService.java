package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.MatchAgendaDTO;
import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoPartida;
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
    private final InspecaoSumoService inspecaoSumoService;

    @Transactional
    public MatchDTO criar(MatchDTO dto) {
        Bracket bracket = buscarBracket(dto.getBracketId());
        validarEstruturaEditavel(bracket);

        Registration a = buscarRegistrationOpcional(dto.getRegistrationAId());
        Registration b = buscarRegistrationOpcional(dto.getRegistrationBId());
        validarParticipantes(bracket, a, b);
        validarDuplicidade(dto, null);

        Match match = new Match();
        preencherEstrutura(match, dto, bracket, a, b);
        match.setDataHora(dto.getDataHora());
        match.setStatusConvocacao(StatusConvocacaoPartida.NAO_CONVOCADA);
        match.setStatus(dto.getStatus() != null ? dto.getStatus() : definirStatusInicial(a, b));
        match.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new MatchDTO(matchRepository.save(match));
    }

    @Transactional(readOnly = true)
    public List<MatchDTO> listarTodos() {
        return matchRepository
                .findAll()
                .stream()
                .map(MatchDTO::new)
                .toList();
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
        validarEstruturaEditavel(match.getBracket());
        validarEstruturaEditavel(bracket);

        Registration a = buscarRegistrationOpcional(dto.getRegistrationAId());
        Registration b = buscarRegistrationOpcional(dto.getRegistrationBId());
        validarParticipantes(bracket, a, b);
        validarDuplicidade(dto, id);
        preencherEstrutura(match, dto, bracket, a, b);
        match.setDataHora(dto.getDataHora());
        if (dto.getStatus() != null) match.setStatus(dto.getStatus());
        if (dto.getAtivo() != null) match.setAtivo(dto.getAtivo());
        return new MatchDTO(matchRepository.save(match));
    }

    @Transactional
    public MatchDTO atualizarAgenda(Long id, MatchAgendaDTO dto) {
        Match match = buscarMatch(id);
        validarBracketOperavel(match.getBracket());
        validarAgendaEditavel(match);

        match.setDataHora(dto.getDataHora());
        match.setPista(normalizar(dto.getPista()));
        match.setOrdemExecucao(dto.getOrdemExecucao());
        match.setStatusConvocacao(dto.getStatusConvocacao() != null
                ? dto.getStatusConvocacao()
                : StatusConvocacaoPartida.NAO_CONVOCADA);

        return new MatchDTO(matchRepository.save(match));
    }

    @Transactional
    public void deletar(Long id) {
        Match match = buscarMatch(id);
        validarEstruturaEditavel(match.getBracket());
        match.setAtivo(false);
        match.setStatus(StatusMatch.CANCELADA);
        match.setStatusConvocacao(StatusConvocacaoPartida.NAO_CONVOCADA);
        matchRepository.save(match);
    }

    @Transactional
    public MatchDTO reativar(Long id) {
        Match match = buscarMatch(id);
        validarEstruturaEditavel(match.getBracket());
        validarParticipantes(match.getBracket(), match.getRegistrationA(), match.getRegistrationB());
        match.setAtivo(true);
        match.setStatus(definirStatusInicial(match.getRegistrationA(), match.getRegistrationB()));
        match.setStatusConvocacao(StatusConvocacaoPartida.NAO_CONVOCADA);
        return new MatchDTO(matchRepository.save(match));
    }

    private void validarParticipantes(Bracket bracket, Registration a, Registration b) {
        validarBracketOperavel(bracket);
        if (a == null && b == null) throw new IllegalArgumentException("A partida deve possuir pelo menos um participante.");
        if (a != null) validarRegistrationDoBracket(bracket, a);
        if (b != null) validarRegistrationDoBracket(bracket, b);
        if (a != null && b != null && a.getId().equals(b.getId()))
            throw new IllegalArgumentException("Os participantes da partida devem ser diferentes.");
    }

    private void validarEstruturaEditavel(Bracket bracket) {
        validarBracketOperavel(bracket);
        if (bracket.getStatus() != StatusBracket.RASCUNHO) {
            throw new IllegalArgumentException(
                    "A estrutura lógica de uma chave gerada não pode ser editada pelo fluxo comum.");
        }
    }

    private void validarAgendaEditavel(Match match) {
        if (!Boolean.TRUE.equals(match.getAtivo())) {
            throw new IllegalArgumentException("Partida inativa não pode ter a agenda alterada.");
        }
        if (match.getStatus() == StatusMatch.EM_ANDAMENTO
                || match.getStatus() == StatusMatch.FINALIZADA
                || match.getStatus() == StatusMatch.CANCELADA
                || match.getStatus() == StatusMatch.BYE) {
            throw new IllegalArgumentException(
                    "A agenda não pode ser alterada depois que a partida foi iniciada, encerrada ou definida como BYE.");
        }
    }

    private void validarBracketOperavel(Bracket bracket) {
        validarBracketSumo(bracket);
        if (!Boolean.TRUE.equals(bracket.getAtivo())) {
            throw new IllegalArgumentException("Chaveamento inativo.");
        }
        if (!Boolean.TRUE.equals(bracket.getAtual())) {
            throw new IllegalArgumentException("Chaveamentos históricos são somente leitura.");
        }
    }

    private void validarBracketSumo(Bracket bracket) {
        if (bracket.getCategory().getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException(
                    "Partidas são exclusivas da modalidade SUMO. FOLLOW_LINE é disputado por ranking de tempos.");
        }
    }

    private void validarRegistrationDoBracket(Bracket bracket, Registration registration) {
        if (!Boolean.TRUE.equals(registration.getAtivo()) || registration.getStatus() != StatusRegistration.APROVADA)
            throw new IllegalArgumentException("Participante deve possuir inscrição ativa e aprovada.");
        if (!registration.getCompetition().getId().equals(bracket.getCompetition().getId())
                || !registration.getCategory().getId().equals(bracket.getCategory().getId()))
            throw new IllegalArgumentException("A inscrição não pertence à competição e categoria do chaveamento.");
        if (!inspecaoSumoService.estaAptaParaCompetir(registration.getId()))
            throw new IllegalArgumentException("Participante de Sumô não está apto para competir.");
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

    private Match buscarMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + id));
    }

    private Bracket buscarBracket(Long id) {
        return bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + id));
    }

    private Registration buscarRegistrationOpcional(Long id) {
        if (id == null) return null;
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private void preencherEstrutura(Match entity, MatchDTO dto, Bracket bracket, Registration a, Registration b) {
        entity.setBracket(bracket);
        entity.setRodada(dto.getRodada());
        entity.setOrdem(dto.getOrdem());
        entity.setRegistrationA(a);
        entity.setRegistrationB(b);
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
