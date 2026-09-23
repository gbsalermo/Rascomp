package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionAdminCatalogDTO;
import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionAdminCatalogService {

    private final CompetitionContextService competitionContextService;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public CompetitionAdminCatalogDTO buscar(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);

        return new CompetitionAdminCatalogDTO(
                competitionId,
                registrationRepository.findTeamsByCompetitionId(competitionId)
                        .stream().map(TeamDTO::new).toList(),
                registrationRepository.findCompetitorsByCompetitionId(competitionId)
                        .stream().map(CompetitorDTO::new).toList(),
                registrationRepository.findRobotsByCompetitionId(competitionId)
                        .stream().map(RobotDTO::new).toList());
    }
}
