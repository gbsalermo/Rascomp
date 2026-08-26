package br.edu.ufrb.rascomp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
            melhorTomada(registration).ifPresent(tentativaRepresentante -> {
                BigDecimal tempoFinal = calcularTempoFinal(tentativaRepresentante);

                ranking.add(new RankingFollowDTO(
                        null,
                        registration.getId(),
                        registration.getRobot().getId(),
                        registration.getRobot().getNome(),
                        registration.getTeam().getNome(),
                        tentativaRepresentante.getTempoSegundos(),
                        tentativaRepresentante.getPenalidadeSegundos(),
                        tempoFinal,
                        tentativaRepresentante.getTomada(),
                        tentativaRepresentante.getNumeroTentativa()
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

    /**
     * Regra de classificacao do Follow Line:
     * 1) cada tomada e representada pela sua melhor tentativa valida e concluida;
     * 2) entre as tomadas do robo, entra no ranking a melhor tomada;
     * 3) o DTO preserva numeroTomada e numeroTentativa para auditoria da passagem
     *    que representou aquela tomada.
     */
    private Optional<TentativaSeguidorLinha> melhorTomada(Registration registration) {
        Map<Integer, List<TentativaSeguidorLinha>> porTomada = tentativaRepository
                .findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(registration.getId())
                .stream()
                .filter(this::tentativaClassificavel)
                .collect(Collectors.groupingBy(TentativaSeguidorLinha::getTomada));

        return porTomada.values().stream()
                .map(this::melhorTentativaDaTomada)
                .flatMap(Optional::stream)
                .min(comparadorTentativas());
    }

    private Optional<TentativaSeguidorLinha> melhorTentativaDaTomada(List<TentativaSeguidorLinha> tentativas) {
        return tentativas.stream().min(comparadorTentativas());
    }

    private boolean tentativaClassificavel(TentativaSeguidorLinha tentativa) {
        return Boolean.TRUE.equals(tentativa.getValida())
                && Boolean.TRUE.equals(tentativa.getConcluida())
                && tentativa.getTempoSegundos() != null;
    }

    private Comparator<TentativaSeguidorLinha> comparadorTentativas() {
        return Comparator.comparing(this::calcularTempoFinal)
                .thenComparing(TentativaSeguidorLinha::getTempoSegundos)
                .thenComparing(TentativaSeguidorLinha::getTomada)
                .thenComparing(TentativaSeguidorLinha::getNumeroTentativa);
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
