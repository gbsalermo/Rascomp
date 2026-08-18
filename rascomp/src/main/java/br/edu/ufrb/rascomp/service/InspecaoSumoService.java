package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.InspecaoSumoDTO;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.InspecaoSumo;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import br.edu.ufrb.rascomp.repository.InspecaoSumoRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InspecaoSumoService {

    private final InspecaoSumoRepository inspecaoRepository;
    private final RegistrationRepository registrationRepository;
    private final ConfigSumoRepository configSumoRepository;

    @Transactional
    public InspecaoSumoDTO registrar(InspecaoSumoDTO dto) {
        Registration registration = buscarRegistration(dto.getRegistrationId());
        validarRegistration(registration);

        ConfigSumo config = buscarConfig(registration);

        if (inspecaoRepository.existsByRegistrationIdAndAprovadaTrue(registration.getId())) {
            throw new IllegalArgumentException("A inscrição já possui inspeção aprovada.");
        }

        long tentativasRealizadas = inspecaoRepository.countByRegistrationId(registration.getId());
        int numeroTentativa = Math.toIntExact(tentativasRealizadas + 1);

        if (numeroTentativa > config.getMaxTentativasInspecao()) {
            throw new IllegalArgumentException("Limite máximo de tentativas de inspeção atingido.");
        }

        boolean aprovada = dto.getPesoMedido().compareTo(config.getPesoMax()) <= 0;

        InspecaoSumo inspecao = new InspecaoSumo();
        inspecao.setRegistration(registration);
        inspecao.setNumeroTentativa(numeroTentativa);
        inspecao.setPesoMedido(dto.getPesoMedido());
        inspecao.setAprovada(aprovada);
        inspecao.setObservacao(normalizar(dto.getObservacao()));

        InspecaoSumo salva = inspecaoRepository.save(inspecao);

        if (!aprovada && numeroTentativa == config.getMaxTentativasInspecao()) {
            registration.setStatus(StatusRegistration.DESCLASSIFICADA);
            registrationRepository.save(registration);
        }

        return new InspecaoSumoDTO(salva);
    }

    @Transactional(readOnly = true)
    public InspecaoSumoDTO buscarPorId(Long id) {
        return new InspecaoSumoDTO(buscarInspecao(id));
    }

    @Transactional(readOnly = true)
    public List<InspecaoSumoDTO> listarPorInscricao(Long registrationId) {
        buscarRegistration(registrationId);
        return inspecaoRepository.findByRegistrationIdOrderByNumeroTentativaAsc(registrationId)
                .stream()
                .map(InspecaoSumoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public InspecaoSumoDTO buscarUltimaPorInscricao(Long registrationId) {
        buscarRegistration(registrationId);
        return inspecaoRepository.findFirstByRegistrationIdOrderByNumeroTentativaDesc(registrationId)
                .map(InspecaoSumoDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Nenhuma inspeção encontrada para a inscrição: " + registrationId));
    }

    @Transactional(readOnly = true)
    public boolean estaAptaParaCompetir(Long registrationId) {
        Registration registration = buscarRegistration(registrationId);
        validarModalidade(registration);
        ConfigSumo config = buscarConfig(registration);

        if (!Boolean.TRUE.equals(registration.getAtivo())
                || registration.getStatus() != StatusRegistration.APROVADA) {
            return false;
        }

        if (!Boolean.TRUE.equals(config.getExigeInspecao())) {
            return true;
        }

        return inspecaoRepository.existsByRegistrationIdAndAprovadaTrue(registrationId);
    }

    private void validarRegistration(Registration registration) {
        validarModalidade(registration);

        if (!Boolean.TRUE.equals(registration.getAtivo())) {
            throw new IllegalArgumentException("A inscrição deve estar ativa.");
        }

        if (registration.getStatus() != StatusRegistration.APROVADA) {
            throw new IllegalArgumentException("A inscrição deve estar aprovada para realizar inspeção.");
        }
    }

    private void validarModalidade(Registration registration) {
        if (registration.getCategory().getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException("Inspeção de Sumô só pode ser registrada para categorias SUMO.");
        }
    }

    private ConfigSumo buscarConfig(Registration registration) {
        Long categoryId = registration.getCategory().getId();
        return configSumoRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Sumô não encontrada para a categoria: " + categoryId));
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private InspecaoSumo buscarInspecao(Long id) {
        return inspecaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inspeção de Sumô não encontrada: " + id));
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
