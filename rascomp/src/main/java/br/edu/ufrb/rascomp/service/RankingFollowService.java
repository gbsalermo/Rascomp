package br.edu.ufrb.rascomp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RankingFollowDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingFollowService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;

    @Transactional(readOnly = true)
    public List<RankingFollowDTO> gerarRanking(Long competitionId, Long categoryId) {
        buscarCompetition(competitionId);
        CompetitionCategory category = buscarCategory(categoryId);
        validarCategoriaFollow(category);

        List<Registration> inscricoes = registrationRepository
                .findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream()
                .filter(registration -> registration.getCategory().getId().equals(categoryId))
                .filter(registration -> Boolean.TRUE.equals(registration.getAtivo()))
                .filter(registration -> registration.getStatus() == StatusRegistration.APROVADA)
                .toList();

        List<RankingFollowDTO> ranking = new ArrayList<>();

        for (Registration registration : inscricoes) {
            melhorTentativa(registration).ifPresent(tentativa -> {
                BigDecimal tempoFinal = calcularTempoFinal(tentativa);

                ranking.add(new RankingFollowDTO(
                        null,
                        registration.getId(),
                        registration.getRobot().getId(),
                        registration.getRobot().getNome(),
                        registration.getTeam().getNome(),
                        tentativa.getTempoSegundos(),
                        tentativa.getPenalidadeSegundos(),
                        tempoFinal,
                        tentativa.getTomada(),
                        tentativa.getNumeroTentativa()
                ));
            });
        }

        ranking.sort(
                Comparator.comparing(RankingFollowDTO::getTempoFinalSegundos)
                        .thenComparing(RankingFollowDTO::getTempoBrutoSegundos)
                        .thenComparing(RankingFollowDTO::getRegistrationId)
        );

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosicao(i + 1);
        }

        return ranking;
    }

    private java.util.Optional<TentativaSeguidorLinha> melhorTentativa(Registration registration) {
        return tentativaRepository
                .findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(registration.getId())
                .stream()
                .filter(tentativa -> Boolean.TRUE.equals(tentativa.getValida()))
                .filter(tentativa -> Boolean.TRUE.equals(tentativa.getConcluida()))
                .filter(tentativa -> tentativa.getTempoSegundos() != null)
                .min(
                        Comparator.comparing(this::calcularTempoFinal)
                                .thenComparing(TentativaSeguidorLinha::getTempoSegundos)
                                .thenComparing(TentativaSeguidorLinha::getTomada)
                                .thenComparing(TentativaSeguidorLinha::getNumeroTentativa)
                );
    }

    private BigDecimal calcularTempoFinal(TentativaSeguidorLinha tentativa) {
        int penalidade = tentativa.getPenalidadeSegundos() != null
                ? tentativa.getPenalidadeSegundos()
                : 0;

        return tentativa.getTempoSegundos().add(BigDecimal.valueOf(penalidade));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }

    private void validarCategoriaFollow(CompetitionCategory category) {
        if (!Boolean.TRUE.equals(category.getAtivo())) {
            throw new IllegalArgumentException("A categoria está inativa.");
        }

        if (category.getModalidade() != Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException("O ranking solicitado é exclusivo da modalidade FOLLOW_LINE.");
        }
    }
}
