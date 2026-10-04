package br.edu.ufrb.rascomp.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.AgendaActivityDTO;
import br.edu.ufrb.rascomp.model.FollowTakeSchedule;
import br.edu.ufrb.rascomp.model.Match;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusMatch;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleEntryRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleRepository;
import br.edu.ufrb.rascomp.repository.MatchRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionAgendaService {

    private final FollowTakeScheduleRepository followScheduleRepository;
    private final FollowTakeScheduleEntryRepository followEntryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final MatchRepository matchRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional(readOnly = true)
    public List<AgendaActivityDTO> listar(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);

        List<AgendaActivityDTO> items = new ArrayList<>();

        followScheduleRepository
                .findByCompetitionIdAndAtivoTrueOrderByDataHoraAscOrdemExecucaoAsc(competitionId)
                .stream()
                .map(this::fromFollow)
                .forEach(items::add);

        matchRepository.findAgendaAtualByCompetitionId(competitionId)
                .stream()
                .filter(match -> match.getStatus() != StatusMatch.BYE
                        && match.getStatus() != StatusMatch.CANCELADA
                        && match.getStatus() != StatusMatch.AGUARDANDO_PARTICIPANTES)
                .map(this::fromMatch)
                .forEach(items::add);

        return items.stream()
                .sorted(
                        Comparator.comparing(
                                AgendaActivityDTO::getDataHora,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                AgendaActivityDTO::getOrdemExecucao,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AgendaActivityDTO::getTipo)
                        .thenComparing(AgendaActivityDTO::getSourceId))
                .toList();
    }

    private AgendaActivityDTO fromFollow(FollowTakeSchedule schedule) {
        AgendaActivityDTO dto = new AgendaActivityDTO();
        dto.setTipo("FOLLOW_TAKE");
        dto.setSourceId(schedule.getId());
        dto.setCompetitionId(schedule.getCompetition().getId());
        dto.setCompetitionNome(schedule.getCompetition().getNome());
        dto.setCategoryId(schedule.getCategory().getId());
        dto.setCategoryNome(schedule.getCategory().getNome());
        dto.setModalidade(Modalidade.FOLLOW_LINE);
        Integer numeroTomadas = configFollowRepository
                .findByCompetitionCategoryId(schedule.getCategory().getId())
                .map(config -> config.getNumeroTomadas())
                .orElse(0);
        boolean extra = schedule.getTomada() != null && schedule.getTomada() > numeroTomadas;
        dto.setTitulo(
                (extra ? "Tomada Extra " : "Tomada ")
                        + schedule.getTomada()
                        + " · "
                        + schedule.getCategory().getNome());
        dto.setDataHora(schedule.getDataHora());
        dto.setPista(schedule.getPista());
        dto.setOrdemExecucao(schedule.getOrdemExecucao());
        dto.setStatus(schedule.getStatus().name());
        dto.setTomada(schedule.getTomada());
        dto.setTotalFila(Math.toIntExact(
                followEntryRepository.findByScheduleIdOrderByOrdemConvocacaoAsc(schedule.getId()).size()));
        dto.setConcluidos(Math.toIntExact(
                followEntryRepository.countByScheduleIdAndStatus(
                        schedule.getId(), StatusConvocacaoFollow.CONCLUIDA)));
        dto.setAusentes(Math.toIntExact(
                followEntryRepository.countByScheduleIdAndStatus(
                        schedule.getId(), StatusConvocacaoFollow.AUSENTE)));
        return dto;
    }

    private AgendaActivityDTO fromMatch(Match match) {
        AgendaActivityDTO dto = new AgendaActivityDTO();
        dto.setTipo("SUMO_MATCH");
        dto.setSourceId(match.getId());
        dto.setCompetitionId(match.getBracket().getCompetition().getId());
        dto.setCompetitionNome(match.getBracket().getCompetition().getNome());
        dto.setCategoryId(match.getBracket().getCategory().getId());
        dto.setCategoryNome(match.getBracket().getCategory().getNome());
        dto.setModalidade(Modalidade.SUMO);
        String robotA = match.getRegistrationA() != null
                ? match.getRegistrationA().getRobot().getNome()
                : "A definir";
        String robotB = match.getRegistrationB() != null
                ? match.getRegistrationB().getRobot().getNome()
                : "A definir";
        dto.setTitulo(robotA + " × " + robotB);
        dto.setDataHora(match.getDataHora());
        dto.setPista(match.getPista());
        dto.setOrdemExecucao(match.getOrdemExecucao());
        dto.setStatus(
                match.getStatus() == StatusMatch.EM_ANDAMENTO
                        || match.getStatus() == StatusMatch.FINALIZADA
                        ? match.getStatus().name()
                        : (match.getStatusConvocacao() != null
                                ? match.getStatusConvocacao().name()
                                : match.getStatus().name()));
        dto.setBracketId(match.getBracket().getId());
        dto.setMatchId(match.getId());
        return dto;
    }
}
