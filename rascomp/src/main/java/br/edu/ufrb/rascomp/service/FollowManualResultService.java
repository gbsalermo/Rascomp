package br.edu.ufrb.rascomp.service;

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

        Registration winner = registrationRepository.findById(request.getWinnerRegistrationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inscrição vencedora não encontrada: " + request.getWinnerRegistrationId()));

        if (!winner.getCompetition().getId().equals(competition.getId())
                || !winner.getCategory().getId().equals(category.getId())
                || !Boolean.TRUE.equals(winner.getAtivo())
                || winner.getStatus() != StatusRegistration.APROVADA) {
            throw new IllegalArgumentException(
                    "A inscrição escolhida não é elegível para esta decisão de resultado.");
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
        result.setDecidedByUser(operador);
        result.setJustificativa(justificativa);

        return new FollowManualResultDTO(repository.save(result));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<FollowManualResultDTO> buscar(Long competitionId, Long categoryId) {
        competitionContextService.exigirOperavel(competitionId);
        return repository.findByCompetitionIdAndCategoryId(competitionId, categoryId)
                .map(FollowManualResultDTO::new);
    }
}
