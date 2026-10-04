package br.edu.ufrb.rascomp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompetitionAdminCatalogDTO {
    private final Long competitionId;
    private final List<TeamDTO> teams;
    private final List<CompetitorDTO> competitors;
    private final List<RobotDTO> robots;
}
