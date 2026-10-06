package br.edu.ufrb.rascomp.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.dto.CompetitionCategoryDTO;
import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.dto.InstitutionDTO;
import br.edu.ufrb.rascomp.dto.MatchDTO;
import br.edu.ufrb.rascomp.dto.MatchResultDTO;
import br.edu.ufrb.rascomp.dto.FollowTakeScheduleDTO;
import br.edu.ufrb.rascomp.dto.FollowTakeScheduleEntryDTO;
import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.dto.PublicCompetitorDTO;
import br.edu.ufrb.rascomp.dto.PublicCompetitionCategoryResultDTO;
import br.edu.ufrb.rascomp.dto.PublicRegistrationDTO;
import br.edu.ufrb.rascomp.dto.PublicRobotDTO;
import br.edu.ufrb.rascomp.dto.PublicTeamDTO;
import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.dto.RobotImageDTO;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleEntryRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotImageRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicQueryService {

    private static final Set<StatusCompetition> PUBLIC_COMPETITION_STATUSES = Set.of(
            StatusCompetition.INSCRICOES_ABERTAS,
            StatusCompetition.INSCRICOES_ENCERRADAS,
            StatusCompetition.EM_ANDAMENTO);

    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryService categoryService;
    private final InstitutionService institutionService;
    private final TeamRepository teamRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotRepository robotRepository;
    private final RobotImageRepository robotImageRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaSeguidorLinhaRepository;
    private final FollowTakeScheduleRepository followTakeScheduleRepository;
    private final FollowTakeScheduleEntryRepository followTakeScheduleEntryRepository;
    private final RankingFollowService rankingFollowService;
    private final CompetitionResultsService competitionResultsService;
    private final BracketService bracketService;
    private final MatchService matchService;
    private final MatchResultService matchResultService;
    private final RobotImageService robotImageService;

    @Transactional(readOnly = true)
    public List<CompetitionDTO> competicoes() {
        return competitionRepository.findFirstByVigenteTrueAndAtivoTrue()
                .filter(competition -> PUBLIC_COMPETITION_STATUSES.contains(competition.getStatus()))
                .map(competition -> List.of(new CompetitionDTO(competition)))
                .orElseGet(List::of);
    }

    public List<InstitutionDTO> instituicoes() {
        return institutionService.listarAtivas();
    }

    public List<CompetitionCategoryDTO> categorias(Modalidade modalidade) {
        if (modalidade == null) {
            return categoryService.listarTodos().stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getAtivo()))
                    .toList();
        }
        return categoryService.listarPorModalidadeAtiva(modalidade);
    }

    @Transactional(readOnly = true)
    public List<PublicTeamDTO> equipes() {
        return teamRepository.findByAtivoTrueOrderByNomeAsc()
                .stream().map(PublicTeamDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicCompetitorDTO> competidores(Long teamId) {
        return competitorRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId)
                .stream().map(PublicCompetitorDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicRobotDTO> robos(Long teamId) {
        List<Robot> robots = teamId == null
                ? robotRepository.findByAtivoTrueOrderByNomeAsc()
                : robotRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId);

        return robots.stream()
                .map(robot -> new PublicRobotDTO(robot, fotoPrincipal(robot.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RobotImageDTO> fotos(Long robotId) {
        return robotImageService.listar(robotId);
    }

    @Transactional(readOnly = true)
    public List<PublicRegistrationDTO> inscricoes(Long competitionId) {
        return registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream()
                .filter(registration -> Boolean.TRUE.equals(registration.getAtivo()))
                .filter(registration -> registration.getStatus() == StatusRegistration.APROVADA)
                .map(PublicRegistrationDTO::new)
                .toList();
    }

    public List<RankingFollowDTO> rankingFollow(Long competitionId, Long categoryId) {
        return rankingFollowService.gerarRanking(competitionId, categoryId);
    }

    @Transactional(readOnly = true)
    public List<PublicCompetitionCategoryResultDTO> podios(Long competitionId) {
        return competitionResultsService.listarPublico(competitionId)
                .stream()
                .map(PublicCompetitionCategoryResultDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TentativaSeguidorLinhaDTO> tentativasFollow(Long competitionId, Long categoryId) {
        return tentativaSeguidorLinhaRepository
                .findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
                        competitionId, categoryId)
                .stream()
                .map(TentativaSeguidorLinhaDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowTakeScheduleDTO> agendaFollow(Long competitionId, Long categoryId) {
        return followTakeScheduleRepository
                .findByCompetitionIdAndCategoryIdAndAtivoTrueOrderByTomadaAsc(competitionId, categoryId)
                .stream()
                .map(schedule -> {
                    FollowTakeScheduleDTO dto = new FollowTakeScheduleDTO(schedule);
                    dto.setTotalFila(Math.toIntExact(
                            followTakeScheduleEntryRepository
                                    .findByScheduleIdOrderByOrdemConvocacaoAsc(schedule.getId())
                                    .size()));
                    dto.setConcluidos(Math.toIntExact(
                            followTakeScheduleEntryRepository.countByScheduleIdAndStatus(
                                    schedule.getId(),
                                    br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow.CONCLUIDA)));
                    dto.setAusentes(Math.toIntExact(
                            followTakeScheduleEntryRepository.countByScheduleIdAndStatus(
                                    schedule.getId(),
                                    br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow.AUSENTE)));
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowTakeScheduleEntryDTO> filaFollow(Long scheduleId) {
        return followTakeScheduleEntryRepository
                .findByScheduleIdOrderByOrdemConvocacaoAsc(scheduleId)
                .stream()
                .map(FollowTakeScheduleEntryDTO::new)
                .toList();
    }

    public List<BracketDTO> chaveamentos(Long competitionId) {
        return bracketService.listarAtuaisPorCompeticao(competitionId);
    }

    public List<MatchDTO> partidas(Long bracketId) {
        validarBracketPublico(bracketId);
        return matchService.listarPorChaveamento(bracketId);
    }

    public List<MatchResultDTO> resultados(Long bracketId) {
        validarBracketPublico(bracketId);
        return matchResultService.listarPorChaveamento(bracketId);
    }

    public RobotImageService.RobotImageFile arquivoFoto(Long robotId, Long imageId) {
        return robotImageService.carregarPublico(robotId, imageId);
    }

    private void validarBracketPublico(Long bracketId) {
        BracketDTO bracket = bracketService.buscarPorId(bracketId);
        if (!Boolean.TRUE.equals(bracket.getAtivo()) || !Boolean.TRUE.equals(bracket.getAtual())) {
            throw new EntityNotFoundException("Chaveamento público não encontrado: " + bracketId);
        }
    }

    private String fotoPrincipal(Long robotId) {
        return robotImageRepository.findFirstByRobotIdAndPrincipalTrueAndAtivoTrue(robotId)
                .map(image -> "/api/v1/public/robos/" + robotId + "/fotos/" + image.getId() + "/arquivo")
                .orElse(null);
    }
}
