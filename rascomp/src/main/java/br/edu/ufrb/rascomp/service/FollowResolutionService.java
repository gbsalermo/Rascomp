package br.edu.ufrb.rascomp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.FollowTakeSchedule;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.StatusChamadaFollow;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.FollowManualResultRepository;
import br.edu.ufrb.rascomp.repository.FollowTakeScheduleRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowResolutionService {

    private final ConfigFollowRepository configFollowRepository;
    private final RegistrationRepository registrationRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final AusenciaTomadaSeguidorLinhaRepository ausenciaRepository;
    private final FollowTakeScheduleRepository scheduleRepository;
    private final FollowManualResultRepository manualResultRepository;

    @Transactional(readOnly = true)
    public boolean programaNormalConcluido(Long competitionId, Long categoryId) {
        ConfigFollow config = buscarConfig(categoryId);
        return programaConcluidoNasTomadas(
                competitionId,
                categoryId,
                java.util.stream.IntStream.rangeClosed(1, config.getNumeroTomadas())
                        .boxed()
                        .toList(),
                config);
    }

    @Transactional(readOnly = true)
    public boolean programaConcluido(Long competitionId, Long categoryId) {
        ConfigFollow config = buscarConfig(categoryId);
        List<Integer> tomadas = new ArrayList<>(
                java.util.stream.IntStream.rangeClosed(1, config.getNumeroTomadas())
                        .boxed()
                        .toList());

        if (tomadaExtraAtiva(competitionId, categoryId)) {
            tomadas.add(config.getNumeroTomadas() + 1);
        }

        return programaConcluidoNasTomadas(competitionId, categoryId, tomadas, config);
    }

    @Transactional(readOnly = true)
    public boolean possuiTentativaClassificavel(Long competitionId, Long categoryId) {
        return tentativaRepository
                .findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
                        competitionId,
                        categoryId)
                .stream()
                .anyMatch(item ->
                        Boolean.TRUE.equals(item.getValida())
                                && Boolean.TRUE.equals(item.getConcluida())
                                && item.getTempoSegundos() != null);
    }

    @Transactional(readOnly = true)
    public int numeroTomadaExtra(Long categoryId) {
        return buscarConfig(categoryId).getNumeroTomadas() + 1;
    }

    @Transactional(readOnly = true)
    public boolean tomadaExtraAtiva(Long competitionId, Long categoryId) {
        int extra = numeroTomadaExtra(categoryId);
        return scheduleRepository
                .findByCompetitionIdAndCategoryIdAndTomada(competitionId, categoryId, extra)
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> item.getStatus() != StatusChamadaFollow.CANCELADA)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean tomadaPermitida(Long competitionId, Long categoryId, Integer tomada) {
        if (tomada == null) return false;
        ConfigFollow config = buscarConfig(categoryId);
        if (tomada >= 1 && tomada <= config.getNumeroTomadas()) return true;

        return tomada == config.getNumeroTomadas() + 1
                && tomadaExtraAtiva(competitionId, categoryId);
    }

    @Transactional(readOnly = true)
    public boolean podeCriarTomadaExtra(Long competitionId, Long categoryId) {
        int extra = numeroTomadaExtra(categoryId);
        return programaNormalConcluido(competitionId, categoryId)
                && !possuiTentativaClassificavel(competitionId, categoryId)
                && !manualResultRepository.existsByCompetitionIdAndCategoryId(competitionId, categoryId)
                && !scheduleRepository.existsByCompetitionIdAndCategoryIdAndTomada(
                        competitionId, categoryId, extra);
    }

    @Transactional(readOnly = true)
    public boolean podeDecidirManualmente(Long competitionId, Long categoryId) {
        return programaConcluido(competitionId, categoryId)
                && !possuiTentativaClassificavel(competitionId, categoryId)
                && !manualResultRepository.existsByCompetitionIdAndCategoryId(competitionId, categoryId);
    }

    @Transactional(readOnly = true)
    public void exigirPodeCriarTomadaExtra(Long competitionId, Long categoryId) {
        if (!podeCriarTomadaExtra(competitionId, categoryId)) {
            throw new IllegalArgumentException(
                    "A Tomada Extra só pode ser criada após o programa normal terminar sem nenhuma tentativa classificável.");
        }
    }

    @Transactional(readOnly = true)
    public void exigirPodeDecidirManualmente(Long competitionId, Long categoryId) {
        if (!podeDecidirManualmente(competitionId, categoryId)) {
            throw new IllegalArgumentException(
                    "A decisão administrativa só pode ser registrada quando o programa do Follow estiver encerrado sem resultado classificável.");
        }
    }

    private boolean programaConcluidoNasTomadas(
            Long competitionId,
            Long categoryId,
            List<Integer> tomadas,
            ConfigFollow config) {

        List<Registration> participantes = registrationRepository
                .findByCompetitionIdAndCategoryIdAndStatusAndAtivoTrueOrderByIdAsc(
                        competitionId,
                        categoryId,
                        StatusRegistration.APROVADA);

        if (participantes.isEmpty()) return false;

        for (Registration registration : participantes) {
            for (Integer tomada : tomadas) {
                boolean ausente = ausenciaRepository
                        .existsByRegistrationIdAndTomada(registration.getId(), tomada);
                long tentativas = tentativaRepository
                        .countByRegistrationIdAndTomada(registration.getId(), tomada);

                if (!ausente && tentativas < config.getTentativasPorTomada()) {
                    return false;
                }
            }
        }

        return true;
    }

    private ConfigFollow buscarConfig(Long categoryId) {
        return configFollowRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração Follow não encontrada para a categoria: " + categoryId));
    }
}
