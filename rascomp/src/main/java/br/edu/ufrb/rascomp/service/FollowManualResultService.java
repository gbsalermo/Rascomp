package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.FollowManualResultDTO;
import br.edu.ufrb.rascomp.dto.FollowManualResultRequest;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.FollowManualResult;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowManualResultService {

    private final FollowManualResultRepository repository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final RegistrationRepository registrationRepository;
    private final FollowResolutionService resolutionService;
    private final CompetitionContextService competitionContextService;
    private final UserAccountService userAccountService;

    @Transactional
    public FollowManualResultDTO definir(FollowManualResultRequest request) {
        competitionContextService.exigirOperavel(request.getCompetitionId());

        UserAccount operador = userAccountService.buscarAtual();
        if (!operador.getRole().podeOperarCompeticao()) {
            throw new AccessDeniedException(
                    "Apenas DEV ou GESTÃO podem definir um resultado administrativo do Follow.");
        }

        if (repository.existsByCompetitionIdAndCategoryId(
                request.getCompetitionId(), request.getCategoryId())) {
            throw new IllegalArgumentException(
                    "Esta categoria já possui decisão administrativa registrada.");
        }

        Competition competition = competitionRepository.findById(request.getCompetitionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competição não encontrada: " + request.getCompetitionId()));

        CompetitionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoria não encontrada: " + request.getCategoryId()));

        if (!Boolean.TRUE.equals(category.getAtivo())
                || category.getModalidade() != Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException(
                    "A decisão administrativa só se aplica a categoria FOLLOW_LINE ativa.");
        }

        resolutionService.exigirPodeDecidirManualmente(
                competition.getId(), category.getId());

        List<Registration> elegiveis = registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        competition.getId(),
                        category.getId(),
                        StatusRegistration.APROVADA);

        Registration winner = buscarElegivel(
                request.getWinnerRegistrationId(),
                competition,
                category,
                "campeão");

        Registration second = request.getSecondRegistrationId() == null
                ? null
                : buscarElegivel(request.getSecondRegistrationId(), competition, category, "vice-campeão");

        Registration third = request.getThirdRegistrationId() == null
                ? null
                : buscarElegivel(request.getThirdRegistrationId(), competition, category, "terceiro lugar");

        if (elegiveis.size() >= 2 && second == null) {
            throw new IllegalArgumentException("Defina também o vice-campeão.");
        }
        if (elegiveis.size() >= 3 && third == null) {
            throw new IllegalArgumentException("Defina também o terceiro lugar.");
        }

        if (second != null && winner.getId().equals(second.getId())) {
            throw new IllegalArgumentException("Campeão e vice-campeão devem ser inscrições diferentes.");
        }
        if (third != null && (winner.getId().equals(third.getId())
                || (second != null && second.getId().equals(third.getId())))) {
            throw new IllegalArgumentException("As posições do pódio devem usar inscrições diferentes.");
        }

        String justificativa = request.getJustificativa() == null
                ? ""
                : request.getJustificativa().trim();
        if (justificativa.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe a justificativa da decisão administrativa.");
        }

        FollowManualResult result = new FollowManualResult();
        result.setCompetition(competition);
        result.setCategory(category);
        result.setWinnerRegistration(winner);
        result.setSecondRegistration(second);
        result.setThirdRegistration(third);
        result.setDecidedByUser(operador);
        result.setJustificativa(justificativa);

        return new FollowManualResultDTO(repository.save(result));
    }

    private Registration buscarElegivel(
            Long registrationId,
            Competition competition,
            CompetitionCategory category,
            String posicao) {

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inscrição para " + posicao + " não encontrada: " + registrationId));

        if (!registration.getCompetition().getId().equals(competition.getId())
                || !registration.getCategory().getId().equals(category.getId())
                || !Boolean.TRUE.equals(registration.getAtivo())
                || registration.getStatus() != StatusRegistration.APROVADA) {
            throw new IllegalArgumentException(
                    "A inscrição escolhida para " + posicao + " não é elegível para esta decisão.");
        }
        return registration;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<FollowManualResultDTO> buscar(Long competitionId, Long categoryId) {
        competitionContextService.exigirOperavel(competitionId);
        return repository.findByCompetitionIdAndCategoryId(competitionId, categoryId)
                .map(FollowManualResultDTO::new);
    }
}
