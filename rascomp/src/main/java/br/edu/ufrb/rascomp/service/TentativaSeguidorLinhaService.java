package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.TentativaSeguidorLinhaDTO;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.TentativaSeguidorLinha;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.TentativaSeguidorLinhaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TentativaSeguidorLinhaService {
    private final TentativaSeguidorLinhaRepository tentativaRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional
    public TentativaSeguidorLinhaDTO criar(TentativaSeguidorLinhaDTO dto) {
        Registration registration = buscarRegistration(dto.getRegistrationId());
        validarRegistration(registration);
        validarDuplicidade(dto, null);

        TentativaSeguidorLinha tentativa = new TentativaSeguidorLinha();
        preencher(tentativa, dto, registration);
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
        validarDuplicidade(dto, id);
        preencher(tentativa, dto, registration);
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

    private void preencher(TentativaSeguidorLinha entity, TentativaSeguidorLinhaDTO dto, Registration registration) {
        entity.setRegistration(registration);
        entity.setTomada(dto.getTomada());
        entity.setNumeroTentativa(dto.getNumeroTentativa());
        entity.setTempoSegundos(dto.getTempoSegundos());
        entity.setCheckpointsAlcancados(dto.getCheckpointsAlcancados());
        entity.setPenalidadeSegundos(dto.getPenalidadeSegundos());
        entity.setConcluida(dto.getConcluida());
        entity.setValida(dto.getValida());
        entity.setObservacao(dto.getObservacao() == null || dto.getObservacao().isBlank() ? null : dto.getObservacao().trim());
    }
}
