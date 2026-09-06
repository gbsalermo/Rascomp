package br.edu.ufrb.rascomp.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationCancellationRequestDTO;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RegistrationCancellationRequest;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCancellationRequest;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.RegistrationCancellationRequestRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationCancellationRequestService {

    private final RegistrationCancellationRequestRepository requestRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationService registrationService;
    private final UserAccountService userAccountService;

    @Transactional
    public RegistrationCancellationRequestDTO solicitar(Long registrationId, UserAccount solicitante, String motivo) {
        if (solicitante.getRole() != UserRole.PARTICIPANTE) {
            throw new AccessDeniedException("Somente PARTICIPANTE pode solicitar cancelamento por este fluxo.");
        }

        Registration registration = buscarRegistration(registrationId);
        if (!Boolean.TRUE.equals(registration.getAtivo()) || registration.getStatus() != StatusRegistration.APROVADA) {
            throw new IllegalArgumentException("Somente inscrição APROVADA e ativa pode ter cancelamento solicitado.");
        }

        StatusCompetition competitionStatus = registration.getCompetition().getStatus();
        if (competitionStatus == StatusCompetition.FINALIZADA || competitionStatus == StatusCompetition.CANCELADA) {
            throw new IllegalArgumentException("Não é possível solicitar cancelamento após o encerramento da competição.");
        }

        if (requestRepository.existsByRegistrationIdAndStatus(registrationId, StatusCancellationRequest.PENDENTE)) {
            throw new IllegalArgumentException("Já existe uma solicitação de cancelamento pendente para esta inscrição.");
        }

        RegistrationCancellationRequest request = new RegistrationCancellationRequest();
        request.setRegistration(registration);
        request.setRequestedByUser(solicitante);
        request.setMotivo(normalizarObrigatorio(motivo, "Informe o motivo do cancelamento."));
        request.setStatus(StatusCancellationRequest.PENDENTE);
        return new RegistrationCancellationRequestDTO(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<RegistrationCancellationRequestDTO> listarPorInscricao(Long registrationId) {
        buscarRegistration(registrationId);
        return requestRepository.findByRegistrationIdOrderByDataCadastroDesc(registrationId)
                .stream().map(RegistrationCancellationRequestDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationCancellationRequestDTO> listar(Long competitionId, StatusCancellationRequest status) {
        List<RegistrationCancellationRequest> requests;
        if (competitionId != null && status != null) {
            requests = requestRepository.findByRegistrationCompetitionIdAndStatusOrderByDataCadastroDesc(competitionId, status);
        } else if (competitionId != null) {
            requests = requestRepository.findByRegistrationCompetitionIdOrderByDataCadastroDesc(competitionId);
        } else if (status != null) {
            requests = requestRepository.findByStatusOrderByDataCadastroDesc(status);
        } else {
            requests = requestRepository.findAll().stream()
                    .sorted(Comparator.comparing(RegistrationCancellationRequest::getDataCadastro,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return requests.stream().map(RegistrationCancellationRequestDTO::new).toList();
    }

    @Transactional
    public RegistrationCancellationRequestDTO aprovar(Long requestId, String resposta) {
        RegistrationCancellationRequest request = buscarPendente(requestId);
        UserAccount revisor = exigirOrganizacao();

        registrationService.cancelarAprovadaPorSolicitacao(request.getRegistration().getId());
        concluir(request, StatusCancellationRequest.APROVADA, revisor, resposta);
        return new RegistrationCancellationRequestDTO(requestRepository.save(request));
    }

    @Transactional
    public RegistrationCancellationRequestDTO rejeitar(Long requestId, String resposta) {
        RegistrationCancellationRequest request = buscarPendente(requestId);
        UserAccount revisor = exigirOrganizacao();
        concluir(request, StatusCancellationRequest.REJEITADA, revisor, resposta);
        return new RegistrationCancellationRequestDTO(requestRepository.save(request));
    }

    private void concluir(
            RegistrationCancellationRequest request,
            StatusCancellationRequest status,
            UserAccount revisor,
            String resposta) {
        request.setStatus(status);
        request.setReviewedByUser(revisor);
        request.setReviewedAt(LocalDateTime.now());
        request.setResposta(normalizarOpcional(resposta));
    }

    private RegistrationCancellationRequest buscarPendente(Long id) {
        RegistrationCancellationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitação de cancelamento não encontrada: " + id));
        if (request.getStatus() != StatusCancellationRequest.PENDENTE) {
            throw new IllegalArgumentException("Esta solicitação de cancelamento já foi analisada.");
        }
        if (request.getRegistration().getStatus() != StatusRegistration.APROVADA
                || !Boolean.TRUE.equals(request.getRegistration().getAtivo())) {
            throw new IllegalArgumentException("A inscrição não está mais APROVADA e ativa para receber esta decisão.");
        }
        return request;
    }

    private UserAccount exigirOrganizacao() {
        UserAccount atual = userAccountService.buscarAtual();
        if (atual.getRole() != UserRole.ORGANIZACAO) {
            throw new AccessDeniedException("Apenas a ORGANIZAÇÃO pode analisar solicitações de cancelamento.");
        }
        return atual;
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private String normalizarObrigatorio(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        String normalized = value.trim();
        if (normalized.length() > 500) throw new IllegalArgumentException("O motivo deve ter no máximo 500 caracteres.");
        return normalized;
    }

    private String normalizarOpcional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
