package br.edu.ufrb.rascomp.dto;

import java.time.LocalDateTime;

import br.edu.ufrb.rascomp.model.RegistrationStatusHistory;
import br.edu.ufrb.rascomp.model.Enum.RegistrationStatusChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import lombok.Getter;

@Getter
public class RegistrationStatusHistoryDTO {

    private final Long id;
    private final Long registrationId;
    private final StatusRegistration previousStatus;
    private final StatusRegistration newStatus;
    private final RegistrationStatusChangeType changeType;
    private final Long actorUserId;
    private final String actorUserNome;
    private final String reason;
    private final LocalDateTime dataCadastro;

    public RegistrationStatusHistoryDTO(RegistrationStatusHistory entity) {
        this.id = entity.getId();
        this.registrationId = entity.getRegistration().getId();
        this.previousStatus = entity.getPreviousStatus();
        this.newStatus = entity.getNewStatus();
        this.changeType = entity.getChangeType();

        if (entity.getActorUser() != null) {
            this.actorUserId = entity.getActorUser().getId();
            this.actorUserNome = entity.getActorUser().getNome();
        } else {
            this.actorUserId = null;
            this.actorUserNome = null;
        }

        this.reason = entity.getReason();
        this.dataCadastro = entity.getDataCadastro();
    }
}
