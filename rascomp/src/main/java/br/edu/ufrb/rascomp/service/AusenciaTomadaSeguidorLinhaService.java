package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.AusenciaTomadaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.AusenciaTomadaSeguidorLinha;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.AusenciaTomadaSeguidorLinhaRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AusenciaTomadaSeguidorLinhaService {

    private final AusenciaTomadaSeguidorLinhaRepository ausenciaRepository;
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final RegistrationRepository registrationRepository;
    private final ConfigFollowRepository configFollowRepository;
    private final UserAccountService userAccountService;
    private final CompetitionContextService competitionContextService;
    private final FollowTakeScheduleService followTakeScheduleService;
    private final FollowResolutionService followResolutionService;

    @Transactional
    public AusenciaTomadaSeguidorLinhaDTO marcar(AusenciaTomadaSeguidorLinhaDTO dto) {
        UserAccount organizacao = exigirOperadorCompeticao();
        Registration registration = buscarRegistration(dto.getRegistrationId());
        exigirContexto(registration);
        validarRegistration(registration);

        ConfigFollow config = buscarConfigFollow(registration);
        validarTomada(dto.getTomada(), config, registration);

        if (tentativaRepository.existsByRegistrationIdAndTomada(registration.getId(), dto.getTomada())) {
            throw new IllegalArgumentException(
                    "A tomada já possui tentativa registrada e não pode ser marcada como perdida por ausência.");
        }

        if (ausenciaRepository.existsByRegistrationIdAndTomada(registration.getId(), dto.getTomada())) {
            throw new IllegalArgumentException("Esta tomada já foi marcada como perdida por ausência.");
        }

        AusenciaTomadaSeguidorLinha ausencia = new AusenciaTomadaSeguidorLinha();
        ausencia.setRegistration(registration);
        ausencia.setTomada(dto.getTomada());
        ausencia.setObservacao(dto.getObservacao() == null || dto.getObservacao().isBlank()
                ? null
                : dto.getObservacao().trim());
        ausencia.setRegistradoPor(organizacao);

        AusenciaTomadaSeguidorLinha salva = ausenciaRepository.save(ausencia);
        followTakeScheduleService.registrarAusencia(registration, dto.getTomada());
        return new AusenciaTomadaSeguidorLinhaDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<AusenciaTomadaSeguidorLinhaDTO> listarPorInscricao(Long registrationId) {
        Registration registration = buscarRegistration(registrationId);
        exigirContexto(registration);
        return ausenciaRepository.findByRegistrationIdOrderByTomadaAsc(registrationId)
                .stream()
                .map(AusenciaTomadaSeguidorLinhaDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AusenciaTomadaSeguidorLinhaDTO> listarPorContexto(Long competitionId, Long categoryId) {
        competitionContextService.exigirOperavel(competitionId);
        return ausenciaRepository
                .findByRegistrationCompetitionIdAndRegistrationCategoryIdOrderByDataCadastroDesc(
                        competitionId,
                        categoryId)
                .stream()
                .map(AusenciaTomadaSeguidorLinhaDTO::new)
                .toList();
    }

    private void exigirContexto(Registration registration) {
        competitionContextService.exigirOperavel(registration.getCompetition().getId());
    }

    private UserAccount exigirOperadorCompeticao() {
        UserAccount atual = userAccountService.buscarAtual();
        if (!atual.getRole().podeOperarCompeticao()) {
            throw new AccessDeniedException("Apenas DEV ou GESTÃO podem registrar ausência em uma tomada de Follow.");
        }
        return atual;
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private void validarRegistration(Registration registration) {
        if (!Boolean.TRUE.equals(registration.getAtivo()) || registration.getStatus() != StatusRegistration.APROVADA) {
            throw new IllegalArgumentException("A inscrição deve estar ativa e aprovada.");
        }
        if (registration.getCategory().getModalidade() != Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException("Ausência de tomada só se aplica à modalidade FOLLOW_LINE.");
        }
    }

    private ConfigFollow buscarConfigFollow(Registration registration) {
        Long categoryId = registration.getCategory().getId();
        return configFollowRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Seguidor de Linha não encontrada para a categoria: " + categoryId));
    }

    private void validarTomada(
            Integer tomada,
            ConfigFollow config,
            Registration registration) {

        if (!followResolutionService.tomadaPermitida(
                registration.getCompetition().getId(),
                registration.getCategory().getId(),
                tomada)) {
            throw new IllegalArgumentException(
                    "Tomada inválida. Use uma tomada normal ou uma Tomada Extra previamente autorizada.");
        }
    }
}
