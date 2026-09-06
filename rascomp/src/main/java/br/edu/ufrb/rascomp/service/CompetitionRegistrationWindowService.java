package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionRegistrationWindowChangeDTO;
import br.edu.ufrb.rascomp.dto.CompetitionRegistrationWindowChangeRequest;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionRegistrationWindowChange;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.RegistrationWindowChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRegistrationWindowChangeRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import br.edu.ufrb.rascomp.repository.MatchResultRepository;
import br.edu.ufrb.rascomp.repository.RoundSumoRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionRegistrationWindowService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionRegistrationWindowChangeRepository changeRepository;
    private final BracketRepository bracketRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final AusenciaTomadaSeguidorLinhaRepository ausenciaFollowRepository;
    private final RoundSumoRepository roundSumoRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchRepository matchRepository;
    private final UserAccountService userAccountService;

    @Transactional
    public CompetitionRegistrationWindowChangeDTO alterar(Long competitionId, CompetitionRegistrationWindowChangeRequest request) {
        UserAccount atual = exigirOrganizacao();
        Competition competition = buscarCompetition(competitionId);

        if (!Boolean.TRUE.equals(competition.getAtivo())) {
            throw new IllegalArgumentException("Competição inativa não pode ter inscrições prorrogadas ou reabertas.");
        }

        RegistrationWindowChangeType tipo;
        if (competition.getStatus() == StatusCompetition.INSCRICOES_ABERTAS) {
            tipo = RegistrationWindowChangeType.PRORROGACAO;
        } else if (competition.getStatus() == StatusCompetition.INSCRICOES_ENCERRADAS) {
            tipo = RegistrationWindowChangeType.REABERTURA;
            validarSemAtividadeCompetitiva(competitionId);
            invalidarChavesAtuais(competitionId);
        } else {
            throw new IllegalArgumentException(
                    "A janela de inscrições só pode ser prorrogada quando estiver aberta ou reaberta quando estiver encerrada.");
        }

        LocalDate novaDataFim = request.getNovaDataFim();
        validarNovaData(competition, novaDataFim);
        String motivo = normalizarMotivo(request.getMotivo());

        CompetitionRegistrationWindowChange change = new CompetitionRegistrationWindowChange();
        change.setCompetition(competition);
        change.setTipo(tipo);
        change.setDataFimAnterior(competition.getFimInscricoes());
        change.setNovaDataFim(novaDataFim);
        change.setMotivo(motivo);
        change.setRealizadoPor(atual);

        competition.setFimInscricoes(novaDataFim);
        if (tipo == RegistrationWindowChangeType.REABERTURA) {
            competition.setStatus(StatusCompetition.INSCRICOES_ABERTAS);
        }
        competitionRepository.save(competition);

        return new CompetitionRegistrationWindowChangeDTO(changeRepository.save(change));
    }

    @Transactional(readOnly = true)
    public List<CompetitionRegistrationWindowChangeDTO> historico(Long competitionId) {
        buscarCompetition(competitionId);
        return changeRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream().map(CompetitionRegistrationWindowChangeDTO::new).toList();
    }

    private void validarNovaData(Competition competition, LocalDate novaDataFim) {
        if (novaDataFim == null) {
            throw new IllegalArgumentException("Informe a nova data final das inscrições.");
        }
        if (!novaDataFim.isAfter(competition.getFimInscricoes())) {
            throw new IllegalArgumentException("A nova data final deve ser posterior à data final atual das inscrições.");
        }
        if (novaDataFim.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A nova data final não pode estar no passado.");
        }
        if (novaDataFim.isAfter(competition.getDataInicio())) {
            throw new IllegalArgumentException("As inscrições devem terminar até o início da competição.");
        }
    }

    private void validarSemAtividadeCompetitiva(Long competitionId) {
        boolean houveAtividade = tentativaRepository.existsByRegistrationCompetitionId(competitionId)
                || ausenciaFollowRepository.existsByRegistrationCompetitionId(competitionId)
                || roundSumoRepository.existsByMatchBracketCompetitionId(competitionId)
                || matchResultRepository.existsByMatchBracketCompetitionId(competitionId)
                || matchRepository.existsByBracketCompetitionIdAndStatusIn(
                        competitionId,
                        List.of(StatusMatch.EM_ANDAMENTO, StatusMatch.FINALIZADA));

        if (houveAtividade) {
            throw new IllegalArgumentException(
                    "As inscrições não podem ser reabertas porque já existe atividade competitiva registrada nesta competição.");
        }
    }

    private void invalidarChavesAtuais(Long competitionId) {
        List<Bracket> atuais = bracketRepository.findByCompetitionIdAndAtualTrueAndAtivoTrueOrderByDataCadastroDesc(competitionId);
        if (atuais.isEmpty()) return;

        atuais.forEach(bracket -> {
            bracket.setAtual(false);
            bracket.setStatus(StatusBracket.CANCELADO);
        });
        bracketRepository.saveAll(atuais);
    }

    private UserAccount exigirOrganizacao() {
        UserAccount atual = userAccountService.buscarAtual();
        if (atual.getRole() != UserRole.ORGANIZACAO) {
            throw new AccessDeniedException("Apenas a ORGANIZAÇÃO pode prorrogar ou reabrir inscrições.");
        }
        return atual;
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com o id: " + id));
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo da prorrogação ou reabertura.");
        }
        String normalized = motivo.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("O motivo deve ter no máximo 500 caracteres.");
        }
        return normalized;
    }
}
