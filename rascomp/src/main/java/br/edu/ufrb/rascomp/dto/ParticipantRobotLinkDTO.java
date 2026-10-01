package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantRobotLinkDTO {
    private Long robotId;
    private String robotNome;
    private Long registrationId;
    private Long categoryId;
    private String categoryNome;
    private StatusRegistration registrationStatus;
}
