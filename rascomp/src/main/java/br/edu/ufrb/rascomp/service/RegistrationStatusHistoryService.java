package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationStatusHistoryDTO;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RegistrationStatusHistory;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.RegistrationStatusChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RegistrationStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationStatusHistoryService {

    private final RegistrationStatusHistoryRepository historyRepository;
    private final RegistrationRepository registrationRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional
    public void registrar(
            Registration registration,
            StatusRegistration previousStatus,
            StatusRegistration newStatus,
            RegistrationStatusChangeType changeType,
            String reason) {

        RegistrationStatusHistory history = new RegistrationStatusHistory();
        history.setRegistration(registration);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangeType(changeType);
        history.setActorUser(usuarioAtualOpcional());
        history.setReason(normalizarOpcional(reason));
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<RegistrationStatusHistoryDTO> listar(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inscrição não encontrada com o id: " + registrationId));

        competitionContextService.exigirOperavel(registration.getCompetition().getId());

        return historyRepository.findByRegistrationIdOrderByDataCadastroDesc(registrationId)
                .stream()
                .map(RegistrationStatusHistoryDTO::new)
                .toList();
    }

    private UserAccount usuarioAtualOpcional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authentication.getPrincipal() instanceof UserAccount user ? user : null;
    }

    private String normalizarOpcional(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
