package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import br.edu.ufrb.rascomp.model.ParticipantRegistrationStatusHistory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.ParticipantCompetitionRegistrationStatus;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.ParticipantCompetitionRegistrationRepository;
import br.edu.ufrb.rascomp.repository.ParticipantRegistrationStatusHistoryRepository;
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
    private final ParticipantRegistrationStatusHistoryRepository statusHistoryRepository;
    private final RegistrationCompositionService compositionService;
    private final RegistrationLotService registrationLotService;

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
        entity.setRegistrationLot(registrationLotService.resolverParaNovaInscricao(competition));
        entity.setCompetitor(competitor);
        entity.setRequestedByUser(usuario);
        entity.setStatus(ParticipantCompetitionRegistrationStatus.PENDENTE);
        entity.setObservacao(request.getObservacao());
        entity.setPaymentReceiptStorageKey(receipt.storageKey());
        entity.setPaymentReceiptOriginalName(receipt.originalFilename());
        entity.setPaymentReceiptContentType(receipt.contentType());
        entity.setAtivo(true);

        ParticipantCompetitionRegistration salva = repository.save(entity);
        registrarHistorico(
                salva,
                null,
                ParticipantCompetitionRegistrationStatus.PENDENTE,
                usuario,
                null);
        return toDto(salva);
    }

    @Transactional
    public ParticipantCompetitionRegistrationDTO criarEntradaManualDev(
            Long competitionId,
            Competitor competitor,
            UserAccount dev,
            String justificativa) {

        if (dev == null || dev.getRole() != UserRole.DEV) {
            throw new AccessDeniedException(
                    "A entrada manual de participante é exclusiva do DEV.");
        }

        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetition(competitionId);
        String motivo = normalizarMotivo(justificativa);

        ParticipantCompetitionRegistration entity = repository
                .findByCompetitionIdAndCompetitorId(competitionId, competitor.getId())
                .orElse(null);

        ParticipantCompetitionRegistrationStatus anterior = null;
        if (entity == null) {
            entity = new ParticipantCompetitionRegistration();
            entity.setCompetition(competition);
            entity.setCompetitor(competitor);
            entity.setRequestedByUser(dev);
        } else {
            anterior = entity.getStatus();
            if (anterior == ParticipantCompetitionRegistrationStatus.APROVADA) {
                return toDto(entity);
            }
        }

        entity.setReviewedByUser(dev);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewReason("Entrada manual DEV: " + motivo);
        entity.setObservacao("Participante incluído manualmente pelo DEV.");
        entity.setStatus(ParticipantCompetitionRegistrationStatus.APROVADA);
        entity.setAtivo(true);

        ParticipantCompetitionRegistration salva = repository.save(entity);
        registrarHistorico(
                salva,
                anterior,
                ParticipantCompetitionRegistrationStatus.APROVADA,
                dev,
                entity.getReviewReason());
        compositionService.sincronizarPorInscricaoPessoal(salva, dev);

        return toDto(salva);
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
        if (competicaoIniciada(entity.getCompetition())) {
            throw new IllegalArgumentException(
                    "A inscrição pessoal não pode ser alterada pelo fluxo normal após o início da competição.");
        }

        UserAccount revisor = userAccountService.buscarAtual();
        if (!revisor.getRole().podeOperarCompeticao()) {
            throw new AccessDeniedException("Apenas DEV ou GESTÃO podem analisar inscrições pessoais.");
        }
        if (entity.getStatus() != ParticipantCompetitionRegistrationStatus.PENDENTE) {
            throw new IllegalArgumentException("Somente inscrição pessoal PENDENTE pode ser analisada.");
        }
        if (request.getStatus() != ParticipantCompetitionRegistrationStatus.APROVADA
                && request.getStatus() != ParticipantCompetitionRegistrationStatus.REJEITADA
                && request.getStatus() != ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA) {
            throw new IllegalArgumentException(
                    "A análise deve aprovar, rejeitar ou solicitar correção da inscrição pessoal.");
        }

        if (request.getStatus() == ParticipantCompetitionRegistrationStatus.REJEITADA
                && ehLiderAtualDaEquipe(entity)) {
            throw new IllegalArgumentException(
                    "A inscrição do líder da equipe não pode ser rejeitada diretamente. "
                            + "Solicite correção ao próprio líder ou peça ao DEV para transferir a liderança "
                            + "para outro participante elegível antes da rejeição definitiva.");
        }

        ParticipantCompetitionRegistrationStatus anterior = entity.getStatus();

        if (request.getStatus() == ParticipantCompetitionRegistrationStatus.REJEITADA
                || request.getStatus() == ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA) {
            entity.setReviewReason(normalizarMotivo(request.getMotivo()));
        } else {
            entity.setReviewReason(null);
        }

        entity.setStatus(request.getStatus());
        entity.setReviewedByUser(revisor);
        entity.setReviewedAt(LocalDateTime.now());
        entity.setAtivo(true);

        ParticipantCompetitionRegistration salva = repository.save(entity);
        registrarHistorico(
                salva,
                anterior,
                request.getStatus(),
                revisor,
                salva.getReviewReason());
        compositionService.sincronizarPorInscricaoPessoal(salva, revisor);
        return toDto(salva);
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

        ParticipantCompetitionRegistrationStatus anterior = entity.getStatus();
        entity.setStatus(ParticipantCompetitionRegistrationStatus.CANCELADA);
        entity.setAtivo(false);
        ParticipantCompetitionRegistration salva = repository.save(entity);
        registrarHistorico(
                salva,
                anterior,
                ParticipantCompetitionRegistrationStatus.CANCELADA,
                usuario,
                null);
        compositionService.sincronizarPorInscricaoPessoal(salva, usuario);
    }

    @Transactional
    public ParticipantCompetitionRegistrationDTO reenviarCorrecao(
            Long id,
            MultipartFile comprovante) {

        ParticipantCompetitionRegistration entity = buscar(id);
        UserAccount usuario = userAccountService.buscarAtual();

        if (!entity.getRequestedByUser().getId().equals(usuario.getId())) {
            throw new AccessDeniedException(
                    "Você não pode corrigir a inscrição pessoal de outra pessoa.");
        }
        if (entity.getStatus() != ParticipantCompetitionRegistrationStatus.CORRECAO_SOLICITADA) {
            throw new IllegalArgumentException(
                    "Somente inscrição com CORRECAO_SOLICITADA pode ser reenviada por este fluxo.");
        }
        if (competicaoIniciada(entity.getCompetition())) {
            throw new IllegalArgumentException(
                    "Não é possível corrigir a inscrição depois do início da competição.");
        }

        RegistrationReceiptStorageService.StoredReceipt receipt = receiptStorageService.armazenar(
                "participante",
                entity.getCompetition().getId(),
                entity.getCompetitor().getId(),
                comprovante);

        if (entity.getPaymentReceiptStorageKey() != null) {
            receiptStorageService.remover(entity.getPaymentReceiptStorageKey());
        }

        ParticipantCompetitionRegistrationStatus anterior = entity.getStatus();
        entity.setPaymentReceiptStorageKey(receipt.storageKey());
        entity.setPaymentReceiptOriginalName(receipt.originalFilename());
        entity.setPaymentReceiptContentType(receipt.contentType());
        entity.setStatus(ParticipantCompetitionRegistrationStatus.PENDENTE);
        entity.setAtivo(true);
        entity.setReviewedByUser(null);
        entity.setReviewedAt(null);
        entity.setReviewReason(null);

        ParticipantCompetitionRegistration salva = repository.save(entity);
        registrarHistorico(
                salva,
                anterior,
                ParticipantCompetitionRegistrationStatus.PENDENTE,
                usuario,
                "Correção reenviada pelo participante.");
        compositionService.sincronizarPorInscricaoPessoal(salva, usuario);
        return toDto(salva);
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
    public void exigirInscricaoIniciada(Long competitionId, Long competitorId) {
        ParticipantCompetitionRegistration entity =
                repository.findByCompetitionIdAndCompetitorId(competitionId, competitorId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Faça sua inscrição individual nesta competição antes de inscrever um robô."));

        if (entity.getStatus() != ParticipantCompetitionRegistrationStatus.PENDENTE
                && entity.getStatus() != ParticipantCompetitionRegistrationStatus.APROVADA) {
            throw new IllegalArgumentException(
                    "Sua inscrição individual precisa estar PENDENTE ou APROVADA para liberar a inscrição de robô.");
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

    private boolean ehLiderAtualDaEquipe(ParticipantCompetitionRegistration entity) {
        return entity.getCompetitor().getUserAccount() != null
                && entity.getCompetitor().getTeam().getResponsibleUser() != null
                && entity.getCompetitor().getUserAccount().getId()
                        .equals(entity.getCompetitor().getTeam().getResponsibleUser().getId());
    }

    private boolean competicaoIniciada(Competition competition) {
        if (competition.getStatus() == StatusCompetition.EM_ANDAMENTO
                || competition.getStatus() == StatusCompetition.FINALIZADA
                || competition.getStatus() == StatusCompetition.CANCELADA) {
            return true;
        }
        return competition.getDataInicio() != null
                && !LocalDate.now().isBefore(competition.getDataInicio());
    }

    private void registrarHistorico(
            ParticipantCompetitionRegistration registration,
            ParticipantCompetitionRegistrationStatus anterior,
            ParticipantCompetitionRegistrationStatus novo,
            UserAccount actor,
            String motivo) {
        ParticipantRegistrationStatusHistory history = new ParticipantRegistrationStatusHistory();
        history.setParticipantRegistration(registration);
        history.setPreviousStatus(anterior);
        history.setNewStatus(novo);
        history.setActorUser(actor);
        history.setReason(motivo);
        statusHistoryRepository.save(history);
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo da rejeição.");
        }
        return motivo.trim();
    }
}
