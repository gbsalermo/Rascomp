package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationDTO;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationRequest;
import br.edu.ufrb.rascomp.dto.ParticipantCompetitionRegistrationReviewRequest;
import br.edu.ufrb.rascomp.dto.ParticipantRobotLinkDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.ParticipantCompetitionRegistration;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotResponsibleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantCompetitionRegistrationService {

    private final ParticipantCompetitionRegistrationRepository repository;
    private final CompetitionRepository competitionRepository;
    private final CompetitorRepository competitorRepository;
    private final RobotResponsibleRepository robotResponsibleRepository;
    private final RegistrationRepository registrationRepository;
    private final UserAccountService userAccountService;
    private final CompetitionContextService competitionContextService;
    private final RegistrationReceiptStorageService receiptStorageService;

    @Transactional
    public ParticipantCompetitionRegistrationDTO criarParaParticipanteAtual(
            ParticipantCompetitionRegistrationRequest request,
            MultipartFile comprovante) {

        UserAccount usuario = userAccountService.buscarAtual();
        if (usuario.getRole() != UserRole.PARTICIPANTE) {
            throw new AccessDeniedException("Somente PARTICIPANTE pode enviar inscrição pessoal.");
        }

        Competitor competitor = competitorRepository.findByUserAccountId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Associe sua conta a uma equipe antes de se inscrever na competição."));

        if (!Boolean.TRUE.equals(competitor.getAtivo())) {
            throw new IllegalArgumentException("Seu vínculo de competidor está inativo.");
        }

        Competition competition = buscarCompetition(request.getCompetitionId());
        validarInscricoesAbertas(competition);

        if (repository.existsByCompetitionIdAndCompetitorId(competition.getId(), competitor.getId())) {
            throw new IllegalArgumentException(
                    "Você já possui uma inscrição pessoal nesta competição. Reabra ou acompanhe a inscrição existente.");
        }

        RegistrationReceiptStorageService.StoredReceipt receipt = receiptStorageService.armazenar(
                "participante",
                competition.getId(),
                competitor.getId(),
                comprovante);

        ParticipantCompetitionRegistration entity = new ParticipantCompetitionRegistration();
        entity.setCompetition(competition);
        entity.setCompetitor(competitor);
        entity.setRequestedByUser(usuario);
        entity.setStatus(ParticipantCompetitionRegistrationStatus.PENDENTE);
        entity.setObservacao(request.getObservacao());
        entity.setPaymentReceiptStorageKey(receipt.storageKey());
        entity.setPaymentReceiptOriginalName(receipt.originalFilename());
        entity.setPaymentReceiptContentType(receipt.contentType());
        entity.setAtivo(true);

        return toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ParticipantCompetitionRegistrationDTO> minhas() {
        UserAccount usuario = userAccountService.buscarAtual();
        Competitor competitor = competitorRepository.findByUserAccountId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sua conta ainda não possui vínculo de competidor com uma equipe."));

        return repository.findByCompetitorIdOrderByDataCadastroDesc(competitor.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantCompetitionRegistrationDTO> listarPorCompeticao(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        buscarCompetition(competitionId);
        return repository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ParticipantCompetitionRegistrationDTO revisar(
            Long id,
            ParticipantCompetitionRegistrationReviewRequest request) {

        ParticipantCompetitionRegistration entity = buscar(id);
        competitionContextService.exigirOperavel(entity.getCompetition().getId());

        UserAccount revisor = userAccountService.buscarAtual();
        if (!revisor.getRole().podeOperarCompeticao()) {
            throw new AccessDeniedException("Apenas DEV ou GESTÃO podem analisar inscrições pessoais.");
        }
        if (entity.getStatus() != ParticipantCompetitionRegistrationStatus.PENDENTE) {
            throw new IllegalArgumentException("Somente inscrição pessoal PENDENTE pode ser analisada.");
        }
        if (request.getStatus() != ParticipantCompetitionRegistrationStatus.APROVADA
                && request.getStatus() != ParticipantCompetitionRegistrationStatus.REJEITADA) {
            throw new IllegalArgumentException("A análise deve aprovar ou rejeitar a inscrição pessoal.");
        }

        if (request.getStatus() == ParticipantCompetitionRegistrationStatus.REJEITADA) {
            entity.setReviewReason(normalizarMotivo(request.getMotivo()));
        } else {
            entity.setReviewReason(null);
        }

        entity.setStatus(request.getStatus());
        entity.setReviewedByUser(revisor);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setAtivo(true);
        return toDto(repository.save(entity));
    }

    @Transactional
    public void cancelarMinha(Long id) {
        ParticipantCompetitionRegistration entity = buscar(id);
        UserAccount usuario = userAccountService.buscarAtual();

        if (!entity.getRequestedByUser().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Você não pode cancelar a inscrição pessoal de outra pessoa.");
        }
        if (entity.getStatus() != ParticipantCompetitionRegistrationStatus.PENDENTE) {
            throw new IllegalArgumentException(
                    "A inscrição pessoal só pode ser cancelada diretamente enquanto estiver PENDENTE.");
        }

        entity.setStatus(ParticipantCompetitionRegistrationStatus.CANCELADA);
        entity.setAtivo(false);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public RegistrationReceiptStorageService.ReceiptFile comprovanteDoParticipante(Long id) {
        ParticipantCompetitionRegistration entity = buscar(id);
        UserAccount usuario = userAccountService.buscarAtual();
        if (!entity.getRequestedByUser().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Você não pode acessar o comprovante de outra pessoa.");
        }
        return carregarComprovante(entity);
    }

    @Transactional(readOnly = true)
    public RegistrationReceiptStorageService.ReceiptFile comprovanteAdministrativo(Long id) {
        ParticipantCompetitionRegistration entity = buscar(id);
        competitionContextService.exigirOperavel(entity.getCompetition().getId());
        return carregarComprovante(entity);
    }

    @Transactional(readOnly = true)
    public boolean estaAprovado(Long competitionId, Long competitorId) {
        return repository.existsByCompetitionIdAndCompetitorIdAndStatusAndAtivoTrue(
                competitionId,
                competitorId,
                ParticipantCompetitionRegistrationStatus.APROVADA);
    }

    @Transactional(readOnly = true)
    public void exigirTodosAprovados(Long competitionId, Collection<Competitor> competitors) {
        List<String> pendentes = competitors.stream()
                .filter(c -> !estaAprovado(competitionId, c.getId()))
                .map(Competitor::getNome)
                .toList();

        if (!pendentes.isEmpty()) {
            throw new IllegalArgumentException(
                    "A inscrição do robô só pode ser aprovada quando todos os competidores estiverem "
                            + "com inscrição pessoal APROVADA na mesma competição. Pendentes: "
                            + String.join(", ", pendentes));
        }
    }

    @Transactional(readOnly = true)
    public ParticipantCompetitionRegistration findByCompetitionAndCompetitor(
            Long competitionId,
            Long competitorId) {
        return repository.findByCompetitionIdAndCompetitorId(competitionId, competitorId)
                .orElse(null);
    }

    private ParticipantCompetitionRegistrationDTO toDto(ParticipantCompetitionRegistration entity) {
        ParticipantCompetitionRegistrationDTO dto = new ParticipantCompetitionRegistrationDTO(entity);
        List<ParticipantRobotLinkDTO> links = new ArrayList<>();

        robotResponsibleRepository
                .findByCompetitorIdAndAtivoTrueOrderByRobotNomeAsc(entity.getCompetitor().getId())
                .forEach(responsibility -> {
                    List<Registration> registrations = registrationRepository
                            .findByCompetitionIdAndRobotIdOrderByDataCadastroDesc(
                                    entity.getCompetition().getId(),
                                    responsibility.getRobot().getId());

                    if (registrations.isEmpty()) {
                        links.add(new ParticipantRobotLinkDTO(
                                responsibility.getRobot().getId(),
                                responsibility.getRobot().getNome(),
                                null,
                                null,
                                null,
                                null));
                    } else {
                        registrations.forEach(registration -> links.add(new ParticipantRobotLinkDTO(
                                responsibility.getRobot().getId(),
                                responsibility.getRobot().getNome(),
                                registration.getId(),
                                registration.getCategory().getId(),
                                registration.getCategory().getNome(),
                                registration.getStatus())));
                    }
                });

        dto.setRobots(links);
        return dto;
    }

    private RegistrationReceiptStorageService.ReceiptFile carregarComprovante(
            ParticipantCompetitionRegistration entity) {
        if (entity.getPaymentReceiptStorageKey() == null) {
            throw new EntityNotFoundException("A inscrição pessoal não possui comprovante anexado.");
        }
        return receiptStorageService.carregar(
                entity.getPaymentReceiptStorageKey(),
                entity.getPaymentReceiptContentType(),
                entity.getPaymentReceiptOriginalName());
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private ParticipantCompetitionRegistration buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição pessoal não encontrada: " + id));
    }

    private void validarInscricoesAbertas(Competition competition) {
        if (!Boolean.TRUE.equals(competition.getAtivo())) {
            throw new IllegalArgumentException("Competição inativa.");
        }

        LocalDate hoje = LocalDate.now();
        boolean dentroDaJanela = !hoje.isBefore(competition.getInicioInscricoes())
                && !hoje.isAfter(competition.getFimInscricoes());

        if (competition.getStatus() != StatusCompetition.INSCRICOES_ABERTAS || !dentroDaJanela) {
            throw new IllegalArgumentException("As inscrições não estão abertas para esta competição.");
        }
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo da rejeição.");
        }
        return motivo.trim();
    }
}
