package br.edu.ufrb.rascomp.dto;

import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistrationCompetitorContextDTO {
    private Long competitorId;
    private String competitorNome;
    private boolean robotResponsible;
    private boolean officialCompetitor;
    private Long participantRegistrationId;
    private ParticipantCompetitionRegistrationStatus participantRegistrationStatus;
}
