package br.edu.ufrb.rascomp.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.FollowTakeScheduleDTO;
import br.edu.ufrb.rascomp.dto.FollowTakeScheduleEntryDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.FollowTakeSchedule;
import br.edu.ufrb.rascomp.model.FollowTakeScheduleEntry;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusChamadaFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusConvocacaoFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleEntryRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowTakeScheduleService {

    private final FollowTakeScheduleRepository scheduleRepository;
    private final FollowTakeScheduleEntryRepository entryRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final RegistrationRepository registrationRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional
    public FollowTakeScheduleDTO criar(FollowTakeScheduleDTO dto) {
        competitionContextService.exigirOperavel(dto.getCompetitionId());

        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        ConfigFollow config = validarContexto(competition, category, dto.getTomada());

        if (scheduleRepository.existsByCompetitionIdAndCategoryIdAndTomada(
                competition.getId(), category.getId(), dto.getTomada())) {
            throw new IllegalArgumentException(
                    "Já existe uma chamada de agenda para esta categoria e tomada.");
        }

        FollowTakeSchedule schedule = new FollowTakeSchedule();
        preencher(schedule, dto, competition, category);
        schedule.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusChamadaFollow.AGENDADA);
        schedule.setAtivo(true);

        FollowTakeSchedule salva = scheduleRepository.save(schedule);
        sincronizarFilaInterno(salva);
        return montarDTO(salva);
    }

    @Transactional
    public FollowTakeScheduleDTO atualizar(Long id, FollowTakeScheduleDTO dto) {
        FollowTakeSchedule schedule = buscarSchedule(id);
        competitionContextService.exigirOperavel(schedule.getCompetition().getId());

        if (!schedule.getCompetition().getId().equals(dto.getCompetitionId())
                || !schedule.getCategory().getId().equals(dto.getCategoryId())
                || !schedule.getTomada().equals(dto.getTomada())) {
            throw new IllegalArgumentException(
                    "A chamada não pode ser transferida para outra competição, categoria ou tomada.");
        }

        if (schedule.getStatus() == StatusChamadaFollow.FINALIZADA
                || schedule.getStatus() == StatusChamadaFollow.CANCELADA) {
            throw new IllegalArgumentException(
                    "Chamada finalizada ou cancelada é somente leitura no fluxo comum.");
        }

        validarContexto(schedule.getCompetition(), schedule.getCategory(), schedule.getTomada());
        preencher(schedule, dto, schedule.getCompetition(), schedule.getCategory());
        if (dto.getStatus() != null) schedule.setStatus(dto.getStatus());

        FollowTakeSchedule salva = scheduleRepository.save(schedule);
        sincronizarFilaInterno(salva);
        return montarDTO(salva);
    }

    @Transactional(readOnly = true)
    public FollowTakeScheduleDTO buscarPorId(Long id) {
        FollowTakeSchedule schedule = buscarSchedule(id);
        competitionContextService.exigirOperavel(schedule.getCompetition().getId());
        return montarDTO(schedule);
    }

    @Transactional(readOnly = true)
    public List<FollowTakeScheduleDTO> listarPorCompeticao(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        return scheduleRepository
                .findByCompetitionIdAndAtivoTrueOrderByDataHoraAscOrdemExecucaoAsc(competitionId)
                .stream()
                .map(this::montarDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowTakeScheduleDTO> listarPorCategoria(Long competitionId, Long categoryId) {
        competitionContextService.exigirOperavel(competitionId);
        return scheduleRepository
                .findByCompetitionIdAndCategoryIdAndAtivoTrueOrderByTomadaAsc(
                        competitionId, categoryId)
                .stream()
                .map(this::montarDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowTakeScheduleEntryDTO> listarFila(Long scheduleId) {
        FollowTakeSchedule schedule = buscarSchedule(scheduleId);
        competitionContextService.exigirOperavel(schedule.getCompetition().getId());

        return entryRepository.findByScheduleIdOrderByOrdemConvocacaoAsc(scheduleId)
                .stream()
                .map(FollowTakeScheduleEntryDTO::new)
                .toList();
    }

    @Transactional
    public List<FollowTakeScheduleEntryDTO> sincronizarFila(Long scheduleId) {
        FollowTakeSchedule schedule = buscarSchedule(scheduleId);
        competitionContextService.exigirOperavel(schedule.getCompetition().getId());
        validarChamadaEditavel(schedule);
        sincronizarFilaInterno(schedule);
        return listarFila(scheduleId);
    }

    @Transactional
    public FollowTakeScheduleEntryDTO atualizarConvocacao(
            Long entryId,
            FollowTakeScheduleEntryDTO dto) {

        FollowTakeScheduleEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Convocação Follow não encontrada: " + entryId));

        competitionContextService.exigirOperavel(entry.getSchedule().getCompetition().getId());

        validarChamadaEditavel(entry.getSchedule());

        if (entry.getStatus() == StatusConvocacaoFollow.AUSENTE
                || entry.getStatus() == StatusConvocacaoFollow.CONCLUIDA) {
            throw new IllegalArgumentException(
                    "Convocação concluída ou marcada como ausente é somente leitura no fluxo comum.");
        }

        validarTransicaoConvocacao(entry, dto.getStatus());

        if (dto.getOrdemConvocacao() != null) {
            entry.setOrdemConvocacao(dto.getOrdemConvocacao());
        }
        if (dto.getStatus() != null) {
            entry.setStatus(dto.getStatus());
        }

        FollowTakeScheduleEntry salva = entryRepository.save(entry);
        recalcularStatusChamada(entry.getSchedule());
        return new FollowTakeScheduleEntryDTO(salva);
    }

    @Transactional
    public void registrarTentativa(
            Registration registration,
            Integer tomada,
            boolean tomadaCompleta) {

        localizarEntrada(registration, tomada).ifPresent(entry -> {
            if (entry.getStatus() == StatusConvocacaoFollow.AUSENTE) return;
            entry.setStatus(tomadaCompleta
                    ? StatusConvocacaoFollow.CONCLUIDA
                    : StatusConvocacaoFollow.EM_EXECUCAO);
            entryRepository.save(entry);
            recalcularStatusChamada(entry.getSchedule());
        });
    }

    @Transactional
    public void registrarAusencia(Registration registration, Integer tomada) {
        localizarEntrada(registration, tomada).ifPresent(entry -> {
            entry.setStatus(StatusConvocacaoFollow.AUSENTE);
            entryRepository.save(entry);
            recalcularStatusChamada(entry.getSchedule());
        });
    }

    private java.util.Optional<FollowTakeScheduleEntry> localizarEntrada(
            Registration registration,
            Integer tomada) {
        return entryRepository
                .findByScheduleCompetitionIdAndScheduleCategoryIdAndScheduleTomadaAndRegistrationId(
                        registration.getCompetition().getId(),
                        registration.getCategory().getId(),
                        tomada,
                        registration.getId());
    }

    private void validarChamadaEditavel(FollowTakeSchedule schedule) {
        if (schedule.getStatus() == StatusChamadaFollow.FINALIZADA
                || schedule.getStatus() == StatusChamadaFollow.CANCELADA) {
            throw new IllegalArgumentException(
                    "Chamada finalizada ou cancelada é somente leitura.");
        }
    }

    private void validarTransicaoConvocacao(
            FollowTakeScheduleEntry entry,
            StatusConvocacaoFollow novoStatus) {
        if (novoStatus == null || novoStatus == entry.getStatus()) return;

        if (novoStatus == StatusConvocacaoFollow.CONCLUIDA
                || novoStatus == StatusConvocacaoFollow.AUSENTE
                || novoStatus == StatusConvocacaoFollow.EM_EXECUCAO) {
            throw new IllegalArgumentException(
                    "Conclusão, ausência e execução da tomada são atualizadas pelo fluxo competitivo.");
        }

        boolean permitida =
                entry.getStatus() == StatusConvocacaoFollow.AGUARDANDO
                        && (novoStatus == StatusConvocacaoFollow.CONVOCADA
                                || novoStatus == StatusConvocacaoFollow.EM_APRESENTACAO)
                || entry.getStatus() == StatusConvocacaoFollow.CONVOCADA
                        && novoStatus == StatusConvocacaoFollow.EM_APRESENTACAO;

        if (!permitida) {
            throw new IllegalArgumentException(
                    "Transição de convocação inválida para o estado atual.");
        }
    }

    private void sincronizarFilaInterno(FollowTakeSchedule schedule) {
        List<FollowTakeScheduleEntry> existentes =
                entryRepository.findByScheduleIdOrderByOrdemConvocacaoAsc(schedule.getId());

        int proximaOrdem = existentes.stream()
                .map(FollowTakeScheduleEntry::getOrdemConvocacao)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        List<Registration> elegiveis = registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        schedule.getCompetition().getId(),
                        schedule.getCategory().getId(),
                        StatusRegistration.APROVADA);

        for (Registration registration : elegiveis) {
            boolean existe = existentes.stream()
                    .anyMatch(item -> item.getRegistration().getId().equals(registration.getId()));
            if (existe) continue;

            FollowTakeScheduleEntry entry = new FollowTakeScheduleEntry();
            entry.setSchedule(schedule);
            entry.setRegistration(registration);
            entry.setOrdemConvocacao(proximaOrdem++);
            entry.setStatus(StatusConvocacaoFollow.AGUARDANDO);
            entryRepository.save(entry);
        }
    }

    private void recalcularStatusChamada(FollowTakeSchedule schedule) {
        if (schedule.getStatus() == StatusChamadaFollow.CANCELADA
                || schedule.getStatus() == StatusChamadaFollow.ADIADA) {
            return;
        }

        List<FollowTakeScheduleEntry> fila =
                entryRepository.findByScheduleIdOrderByOrdemConvocacaoAsc(schedule.getId());
        if (fila.isEmpty()) return;

        boolean todosEncerrados = fila.stream().allMatch(item ->
                item.getStatus() == StatusConvocacaoFollow.CONCLUIDA
                        || item.getStatus() == StatusConvocacaoFollow.AUSENTE);
        if (todosEncerrados) {
            schedule.setStatus(StatusChamadaFollow.FINALIZADA);
        } else if (fila.stream().anyMatch(item ->
                item.getStatus() == StatusConvocacaoFollow.EM_EXECUCAO)) {
            schedule.setStatus(StatusChamadaFollow.EM_ANDAMENTO);
        } else if (fila.stream().anyMatch(item ->
                item.getStatus() == StatusConvocacaoFollow.CONVOCADA
                        || item.getStatus() == StatusConvocacaoFollow.EM_APRESENTACAO)) {
            schedule.setStatus(StatusChamadaFollow.EM_CHAMADA);
        }

        scheduleRepository.save(schedule);
    }

    private ConfigFollow validarContexto(
            Competition competition,
            CompetitionCategory category,
            Integer tomada) {

        if (!Boolean.TRUE.equals(competition.getAtivo())) {
            throw new IllegalArgumentException("Competição inativa não pode receber agenda.");
        }
        if (!Boolean.TRUE.equals(category.getAtivo())
                || category.getModalidade() != Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException(
                    "A agenda de tomada só pode usar categoria FOLLOW_LINE ativa.");
        }

        ConfigFollow config = configFollowRepository.findByCompetitionCategoryId(category.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração Follow não encontrada para a categoria: " + category.getId()));

        if (tomada == null || tomada < 1 || tomada > config.getNumeroTomadas()) {
            throw new IllegalArgumentException(
                    "Tomada inválida para a configuração desta categoria.");
        }

        return config;
    }

    private FollowTakeScheduleDTO montarDTO(FollowTakeSchedule schedule) {
        FollowTakeScheduleDTO dto = new FollowTakeScheduleDTO(schedule);
        dto.setTotalFila(Math.toIntExact(
                entryRepository.findByScheduleIdOrderByOrdemConvocacaoAsc(schedule.getId()).size()));
        dto.setConcluidos(Math.toIntExact(
                entryRepository.countByScheduleIdAndStatus(
                        schedule.getId(), StatusConvocacaoFollow.CONCLUIDA)));
        dto.setAusentes(Math.toIntExact(
                entryRepository.countByScheduleIdAndStatus(
                        schedule.getId(), StatusConvocacaoFollow.AUSENTE)));
        return dto;
    }

    private void preencher(
            FollowTakeSchedule schedule,
            FollowTakeScheduleDTO dto,
            Competition competition,
            CompetitionCategory category) {
        schedule.setCompetition(competition);
        schedule.setCategory(category);
        schedule.setTomada(dto.getTomada());
        schedule.setDataHora(dto.getDataHora());
        schedule.setPista(normalizar(dto.getPista()));
        schedule.setOrdemExecucao(dto.getOrdemExecucao());
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private FollowTakeSchedule buscarSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Chamada Follow não encontrada: " + id));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoria não encontrada: " + id));
    }
}
