package br.edu.ufrb.rascomp.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TentativaSeguidorLinhaService {
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final RegistrationRepository registrationRepository;
    private final ConfigFollowRepository configFollowRepository;

    @Transactional
    public TentativaSeguidorLinhaDTO criar(TentativaSeguidorLinhaDTO dto) {
        Registration registration = buscarRegistration(dto.getRegistrationId());
        validarRegistration(registration);

        ConfigFollow config = buscarConfigFollow(registration);
        validarLimites(dto, config);
        validarDuplicidade(dto, null);

        TentativaSeguidorLinha tentativa = new TentativaSeguidorLinha();
        preencher(tentativa, dto, registration, determinarValidade(dto, config));
        return new TentativaSeguidorLinhaDTO(tentativaRepository.save(tentativa));
    }

    @Transactional(readOnly = true)
    public List<TentativaSeguidorLinhaDTO> listarPorInscricao(Long registrationId) {
        buscarRegistration(registrationId);
        return tentativaRepository
                .findByRegistrationIdOrderByTomadaAscNumeroTentativaAsc(registrationId)
                .stream().map(TentativaSeguidorLinhaDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public TentativaSeguidorLinhaDTO buscarPorId(Long id) {
        return new TentativaSeguidorLinhaDTO(buscarTentativa(id));
    }

    @Transactional
    public TentativaSeguidorLinhaDTO atualizar(Long id, TentativaSeguidorLinhaDTO dto) {
        TentativaSeguidorLinha tentativa = buscarTentativa(id);
        Registration registration = buscarRegistration(dto.getRegistrationId());
        validarRegistration(registration);

        ConfigFollow config = buscarConfigFollow(registration);
        validarLimites(dto, config);
        validarDuplicidade(dto, id);

        preencher(tentativa, dto, registration, determinarValidade(dto, config));
        return new TentativaSeguidorLinhaDTO(tentativaRepository.save(tentativa));
    }

    @Transactional
    public void deletar(Long id) {
        tentativaRepository.delete(buscarTentativa(id));
    }

    private void validarRegistration(Registration registration) {
        if (!Boolean.TRUE.equals(registration.getAtivo()) || registration.getStatus() != StatusRegistration.APROVADA)
            throw new IllegalArgumentException("A inscrição deve estar ativa e aprovada.");
        if (registration.getCategory().getModalidade() != Modalidade.FOLLOW_LINE)
            throw new IllegalArgumentException("Tentativas só podem ser registradas para a modalidade FOLLOW_LINE.");
    }

    private ConfigFollow buscarConfigFollow(Registration registration) {
        Long categoryId = registration.getCategory().getId();
        return configFollowRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Seguidor de Linha não encontrada para a categoria: " + categoryId));
    }

    private void validarLimites(TentativaSeguidorLinhaDTO dto, ConfigFollow config) {
        if (dto.getTomada() < 1 || dto.getTomada() > config.getNumeroTomadas()) {
            throw new IllegalArgumentException(
                    "Tomada inválida. Esta categoria permite tomadas de 1 até " + config.getNumeroTomadas() + ".");
        }

        if (dto.getNumeroTentativa() < 1 || dto.getNumeroTentativa() > config.getTentativasPorTomada()) {
            throw new IllegalArgumentException(
                    "Número de tentativa inválido. Cada tomada permite tentativas de 1 até "
                            + config.getTentativasPorTomada() + ".");
        }

        if (dto.getCheckpointsAlcancados() < 0
                || dto.getCheckpointsAlcancados() > config.getNumeroCheckpoints()) {
            throw new IllegalArgumentException(
                    "Quantidade de checkpoints inválida. Esta categoria possui "
                            + config.getNumeroCheckpoints() + " checkpoints.");
        }
    }

    private boolean determinarValidade(TentativaSeguidorLinhaDTO dto, ConfigFollow config) {
        if (dto.getTempoSegundos() != null) {
            BigDecimal tempoMaximo = BigDecimal.valueOf(config.getMaxTempoSegundos());
            if (dto.getTempoSegundos().compareTo(tempoMaximo) > 0) {
                return false;
            }
        }

        return Boolean.TRUE.equals(dto.getValida());
    }

    private void validarDuplicidade(TentativaSeguidorLinhaDTO dto, Long id) {
        boolean existe = id == null
                ? tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativa(dto.getRegistrationId(), dto.getTomada(), dto.getNumeroTentativa())
                : tentativaRepository.existsByRegistrationIdAndTomadaAndNumeroTentativaAndIdNot(dto.getRegistrationId(), dto.getTomada(), dto.getNumeroTentativa(), id);
        if (existe) throw new IllegalArgumentException("Já existe esta tentativa na tomada informada.");
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private TentativaSeguidorLinha buscarTentativa(Long id) {
        return tentativaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tentativa não encontrada: " + id));
    }

    private void preencher(
            TentativaSeguidorLinha entity,
            TentativaSeguidorLinhaDTO dto,
            Registration registration,
            boolean valida) {
        entity.setRegistration(registration);
        entity.setTomada(dto.getTomada());
        entity.setNumeroTentativa(dto.getNumeroTentativa());
        entity.setTempoSegundos(dto.getTempoSegundos());
        entity.setCheckpointsAlcancados(dto.getCheckpointsAlcancados());
        entity.setPenalidadeSegundos(dto.getPenalidadeSegundos());
        entity.setConcluida(dto.getConcluida());
        entity.setValida(valida);
        entity.setObservacao(dto.getObservacao() == null || dto.getObservacao().isBlank() ? null : dto.getObservacao().trim());
    }
}
