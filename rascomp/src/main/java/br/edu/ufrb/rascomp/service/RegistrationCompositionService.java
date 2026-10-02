package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationCompetitorChangeDTO;
import br.edu.ufrb.rascomp.dto.RegistrationCompetitorChangeReviewRequest;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.RegistrationCompetitorChange;
import br.edu.ufrb.rascomp.model.RobotResponsible;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeStatus;
import br.edu.ufrb.rascomp.model.Enum.RegistrationCompetitorChangeType;
import br.edu.ufrb.rascomp.model.Enum.RegistrationStatusChangeType;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.RegistrationCompetitorChangeRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationCompositionService {

    private final RegistrationRepository registrationRepository;
    private final RobotResponsibleRepository robotResponsibleRepository;
    private final ParticipantCompetitionRegistrationRepository participantRegistrationRepository;
    private final RegistrationCompetitorChangeRepository changeRepository;
    private final RegistrationStatusHistoryService statusHistoryService;
    private final UserAccountService userAccountService;
    private final CompetitionContextService competitionContextService;

    @Transactional(readOnly = true)
    public Set<Competitor> prepararComposicaoInicial(Long competitionId, Long robotId) {
        List<RobotResponsible> responsaveis =
                robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(robotId);

        boolean possuiCadastroIniciado = responsaveis.stream()
                .anyMatch(link -> statusPessoal(competitionId, link.getCompetitor().getId())
                        .map(this::podeParticiparDoCadastroDoRobo)
                        .orElse(false));

        if (!possuiCadastroIniciado) {
            throw new IllegalArgumentException(
                    "O robô precisa possuir ao menos um responsável com inscrição individual "
                            + "PENDENTE ou APROVADA nesta competição.");
        }

        Set<Competitor> aprovados = new LinkedHashSet<>();
        for (RobotResponsible link : responsaveis) {
            if (statusPessoal(competitionId, link.getCompetitor().getId())
                    .map(status -> status == ParticipantCompetitionRegistrationStatus.APROVADA)
                    .orElse(false)) {
                aprovados.add(link.getCompetitor());
            }
        }
        return aprovados;
    }

    @Transactional
    public void sincronizarRobot(Long robotId, UserAccount actor, boolean registrarMudancas) {
        List<Registration> registrations = registrationRepository.findByRobotIdAndStatusIn(
                robotId,
                List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA));

        registrations.forEach(registration ->
                sincronizarInterno(registration, actor, registrarMudancas));
    }

    @Transactional
    public void sincronizarPorInscricaoPessoal(
            ParticipantCompetitionRegistration pessoal,
            UserAccount actor) {

        Map<Long, Registration> registrations = new LinkedHashMap<>();

        robotResponsibleRepository
                .findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(pessoal.getCompetitor().getId())
                .forEach(link -> registrationRepository
                        .findByCompetitionIdAndRobotIdAndStatusIn(
                                pessoal.getCompetition().getId(),
                                link.getRobot().getId(),
                                List.of(StatusRegistration.PENDENTE, StatusRegistration.APROVADA))
                        .forEach(registration -> registrations.put(registration.getId(), registration)));

        registrationRepository
                .findByCompetitionIdAndCompetitorIdOrderByDataCadastroDesc(
                        pessoal.getCompetition().getId(),
                        pessoal.getCompetitor().getId())
                .stream()
                .filter(registration -> registration.getStatus() == StatusRegistration.PENDENTE
                        || registration.getStatus() == StatusRegistration.APROVADA)
                .forEach(registration -> registrations.put(registration.getId(), registration));

        registrations.values().forEach(registration ->
                sincronizarInterno(registration, actor, true));
    }

    @Transactional
    public void sincronizarParaAprovacao(Registration registration, UserAccount actor) {
        exigirComposicaoEditavel(registration);
        sincronizarInterno(registration, actor, false);

        if (!registration.getCompetitors().isEmpty()) {
            return;
        }

        long pendentes = contarResponsaveisRecuperaveis(registration);
        if (pendentes > 0) {
            throw new IllegalArgumentException(
                    "O robô ainda não possui responsável pessoalmente APROVADO. "
                            + "Há responsável(is) aguardando aprovação/correção.");
        }

        throw new IllegalArgumentException(
                "O robô não possui responsável elegível para aprovação nesta competição.");
    }

    @Transactional(readOnly = true)
    public List<RegistrationCompetitorChangeDTO> listarPendentes(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        return changeRepository
                .findByRegistrationCompetitionIdAndStatusOrderByDataCadastroDesc(
                        competitionId,
                        RegistrationCompetitorChangeStatus.PENDENTE_REVISAO)
                .stream()
                .map(RegistrationCompetitorChangeDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationCompetitorChangeDTO> listarPorInscricao(Long registrationId) {
        Registration registration = buscarRegistration(registrationId);
        competitionContextService.exigirOperavel(registration.getCompetition().getId());

        return changeRepository.findByRegistrationIdOrderByDataCadastroDesc(registrationId)
                .stream()
                .map(RegistrationCompetitorChangeDTO::new)
                .toList();
    }

    @Transactional
    public RegistrationCompetitorChangeDTO revisarMudanca(
            Long changeId,
            RegistrationCompetitorChangeReviewRequest request) {

        UserAccount reviewer = userAccountService.buscarAtual();
        if (!reviewer.getRole().podeOperarCompeticao()) {
            throw new AccessDeniedException(
                    "Apenas DEV ou GESTÃO podem revisar alterações de composição.");
        }

        if (request.getStatus() != RegistrationCompetitorChangeStatus.MANTIDA
                && request.getStatus() != RegistrationCompetitorChangeStatus.VETADA) {
            throw new IllegalArgumentException(
                    "A alteração deve ser marcada como MANTIDA ou VETADA.");
        }

        RegistrationCompetitorChange change = changeRepository.findById(changeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Alteração de composição não encontrada: " + changeId));

        exigirComposicaoEditavel(change.getRegistration());

        if (request.getStatus() == RegistrationCompetitorChangeStatus.VETADA
                && (request.getMotivo() == null || request.getMotivo().isBlank())) {
            throw new IllegalArgumentException("Informe a justificativa do veto.");
        }

        change.setStatus(request.getStatus());
        change.setReviewedByUser(reviewer);
        change.setReviewedAt(LocalDateTime.now());
        change.setReason(normalizarOpcional(request.getMotivo()));
        RegistrationCompetitorChange salva = changeRepository.save(change);

        sincronizarInterno(change.getRegistration(), reviewer, false);
        return new RegistrationCompetitorChangeDTO(salva);
    }

    @Transactional(readOnly = true)
    public boolean composicaoCongelada(Registration registration) {
        return competicaoIniciada(registration);
    }

    private void sincronizarInterno(
            Registration registration,
            UserAccount actor,
            boolean registrarMudancas) {

        if (competicaoIniciada(registration)) {
            return;
        }

        Set<Competitor> atual = new LinkedHashSet<>(registration.getCompetitors());
        Set<Competitor> desejada = composicaoDesejada(registration);

        if (registrarMudancas) {
            registrarDiferencas(registration, atual, desejada, actor);
        }

        registration.setCompetitors(desejada);
        reavaliarStatus(registration);
        registrationRepository.save(registration);
    }

    private Set<Competitor> composicaoDesejada(Registration registration) {
        Set<Competitor> desejada = new LinkedHashSet<>();

        List<RobotResponsible> responsaveis =
                robotResponsibleRepository.findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(
                        registration.getRobot().getId());

        for (RobotResponsible link : responsaveis) {
            Competitor competitor = link.getCompetitor();
            boolean aprovado = statusPessoal(
                    registration.getCompetition().getId(),
                    competitor.getId())
                    .map(status -> status == ParticipantCompetitionRegistrationStatus.APROVADA)
                    .orElse(false);

            if (aprovado && !adicaoVetada(registration.getId(), competitor.getId())) {
                desejada.add(competitor);
            }
        }

        Set<Long> remocoesProcessadas = new java.util.HashSet<>();
        for (RegistrationCompetitorChange change :
                changeRepository.findByRegistrationIdOrderByDataCadastroDesc(registration.getId())) {
            if (change.getChangeType() != RegistrationCompetitorChangeType.REMOVIDO
                    || !remocoesProcessadas.add(change.getCompetitor().getId())) {
                continue;
            }

            if (change.getStatus() != RegistrationCompetitorChangeStatus.VETADA) {
                continue;
            }

            Competitor competitor = change.getCompetitor();
            boolean aprovado = statusPessoal(
                    registration.getCompetition().getId(),
                    competitor.getId())
                    .map(status -> status == ParticipantCompetitionRegistrationStatus.APROVADA)
                    .orElse(false);

            if (aprovado) {
                desejada.add(competitor);
            }
        }

        return desejada;
    }

    private boolean adicaoVetada(Long registrationId, Long competitorId) {
        return changeRepository
                .findTopByRegistrationIdAndCompetitorIdAndChangeTypeOrderByIdDesc(
                        registrationId,
                        competitorId,
                        RegistrationCompetitorChangeType.ADICIONADO)
                .map(change -> change.getStatus() == RegistrationCompetitorChangeStatus.VETADA)
                .orElse(false);
    }

    private void registrarDiferencas(
            Registration registration,
            Set<Competitor> atual,
            Set<Competitor> desejada,
            UserAccount actor) {

        for (Competitor competitor : desejada) {
            if (!atual.contains(competitor)) {
                registrarMudanca(
                        registration,
                        competitor,
                        RegistrationCompetitorChangeType.ADICIONADO,
                        actor);
            }
        }

        for (Competitor competitor : atual) {
            if (!desejada.contains(competitor)) {
                registrarMudanca(
                        registration,
                        competitor,
                        RegistrationCompetitorChangeType.REMOVIDO,
                        actor);
            }
        }
    }

    private void registrarMudanca(
            Registration registration,
            Competitor competitor,
            RegistrationCompetitorChangeType type,
            UserAccount actor) {

        RegistrationCompetitorChange change = new RegistrationCompetitorChange();
        change.setRegistration(registration);
        change.setCompetitor(competitor);
        change.setChangeType(type);
        change.setStatus(RegistrationCompetitorChangeStatus.PENDENTE_REVISAO);
        change.setActorUser(actor);
        changeRepository.save(change);
    }

    private void reavaliarStatus(Registration registration) {
        if (registration.getStatus() != StatusRegistration.PENDENTE
                && registration.getStatus() != StatusRegistration.APROVADA) {
            return;
        }

        if (!registration.getCompetitors().isEmpty()) {
            return;
        }

        StatusRegistration anterior = registration.getStatus();
        long recuperaveis = contarResponsaveisRecuperaveis(registration);

        if (recuperaveis > 0) {
            if (anterior == StatusRegistration.APROVADA) {
                registration.setStatus(StatusRegistration.PENDENTE);
                registration.setReviewedByUser(null);
                registration.setReviewedAt(null);
                registration.setReviewReason(
                        "Aguardando ao menos um responsável pessoalmente aprovado.");
                statusHistoryService.registrar(
                        registration,
                        anterior,
                        StatusRegistration.PENDENTE,
                        RegistrationStatusChangeType.AJUSTE_COMPOSICAO,
                        registration.getReviewReason());
            }
            return;
        }

        registration.setStatus(StatusRegistration.REJEITADA);
        registration.setReviewReason(
                "Inscrição rejeitada automaticamente: o robô ficou sem responsável elegível.");
        statusHistoryService.registrar(
                registration,
                anterior,
                StatusRegistration.REJEITADA,
                RegistrationStatusChangeType.AJUSTE_COMPOSICAO,
                registration.getReviewReason());
    }

    private long contarResponsaveisRecuperaveis(Registration registration) {
        return robotResponsibleRepository
                .findByRobotIdAndAtivoTrueOrderByCompetitorNomeAsc(registration.getRobot().getId())
                .stream()
                .filter(link -> !adicaoVetada(
                        registration.getId(),
                        link.getCompetitor().getId()))
                .filter(link -> statusPessoal(
                        registration.getCompetition().getId(),
                        link.getCompetitor().getId())
                        .map(this::statusRecuperavel)
                        .orElse(false))
                .count();
    }

    private java.util.Optional<ParticipantCompetitionRegistrationStatus> statusPessoal(
            Long competitionId,
            Long competitorId) {
        return participantRegistrationRepository
                .findByCompetitionIdAndCompetitorId(competitionId, competitorId)
                .map(ParticipantCompetitionRegistration::getStatus);
    }

    private boolean podeParticiparDoCadastroDoRobo(
            ParticipantCompetitionRegistrationStatus status) {
        return status == ParticipantCompetitionRegistrationStatus.PENDENTE
                || status == ParticipantCompetitionRegistrationStatus.APROVADA;
    }

    private boolean statusRecuperavel(ParticipantCompetitionRegistrationStatus status) {
        return status == ParticipantCompetitionRegistrationStatus.PENDENTE
                || status == ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA;
    }

    private boolean competicaoIniciada(Registration registration) {
        StatusCompetition status = registration.getCompetition().getStatus();
        if (status == StatusCompetition.EM_ANDAMENTO
                || status == StatusCompetition.FINALIZADA
                || status == StatusCompetition.CANCELADA) {
            return true;
        }

        LocalDate dataInicio = registration.getCompetition().getDataInicio();
        return dataInicio != null && !LocalDate.now().isBefore(dataInicio);
    }

    private void exigirComposicaoEditavel(Registration registration) {
        if (competicaoIniciada(registration)) {
            throw new IllegalArgumentException(
                    "A composição do robô está congelada porque a competição já iniciou.");
        }
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inscrição não encontrada: " + id));
    }

    private String normalizarOpcional(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= 500
                ? normalized
                : normalized.substring(0, 500);
    }
}
