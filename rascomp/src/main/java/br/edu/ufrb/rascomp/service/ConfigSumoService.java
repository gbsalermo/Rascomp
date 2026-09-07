package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.ConfigSumoDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigSumo;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.ConfigSumoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfigSumoService {

    private static final int ROUNDS_REGULARES_RRC = 3;
    private static final int VITORIAS_PARA_VENCER_RRC = 2;
    private static final int MAX_ROUNDS_EXTRAS_RRC = 2;

    private final ConfigSumoRepository configSumoRepository;
    private final CompetitionCategoryRepository competitionCategoryRepository;

    @Transactional
    public ConfigSumoDTO criar(Long categoryId, ConfigSumoDTO dto) {
        CompetitionCategory category = buscarCategoria(categoryId);
        validarCategoriaAtiva(category);
        validarModalidade(category);
        validarConfigInexistente(categoryId);
        validarInspecao(dto);
        validarRounds(dto);

        ConfigSumo config = new ConfigSumo();
        config.setCompetitionCategory(category);
        preencherConfig(config, dto);
        return new ConfigSumoDTO(configSumoRepository.save(config));
    }

    @Transactional(readOnly = true)
    public ConfigSumoDTO buscarPorCategoria(Long categoryId) {
        return new ConfigSumoDTO(buscarConfigPorCategoria(categoryId));
    }

    @Transactional
    public ConfigSumoDTO atualizar(Long categoryId, ConfigSumoDTO dto) {
        ConfigSumo config = buscarConfigPorCategoria(categoryId);
        CompetitionCategory category = config.getCompetitionCategory();

        validarCategoriaAtiva(category);
        validarModalidade(category);
        validarInspecao(dto);
        validarRounds(dto);
        preencherConfig(config, dto);

        return new ConfigSumoDTO(configSumoRepository.save(config));
    }

    @Transactional
    public void deletar(Long categoryId) {
        configSumoRepository.delete(buscarConfigPorCategoria(categoryId));
    }

    private CompetitionCategory buscarCategoria(Long categoryId) {
        return competitionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com o id: " + categoryId));
    }

    private ConfigSumo buscarConfigPorCategoria(Long categoryId) {
        return configSumoRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Sumô não encontrada para a categoria com o id: " + categoryId));
    }

    private void validarCategoriaAtiva(CompetitionCategory category) {
        if (!Boolean.TRUE.equals(category.getAtivo())) {
            throw new IllegalArgumentException("Não é possível configurar uma categoria inativa.");
        }
    }

    private void validarModalidade(CompetitionCategory category) {
        if (category.getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException("ConfigSumo só pode ser associada a uma categoria da modalidade SUMO.");
        }
    }

    private void validarConfigInexistente(Long categoryId) {
        if (configSumoRepository.existsByCompetitionCategoryId(categoryId)) {
            throw new IllegalArgumentException("A categoria já possui uma configuração de Sumô.");
        }
    }

    private void validarInspecao(ConfigSumoDTO dto) {
        Integer tentativas = dto.getMaxTentativasInspecao();

        if (Boolean.TRUE.equals(dto.getExigeInspecao()) && (tentativas == null || tentativas < 1)) {
            throw new IllegalArgumentException(
                    "Quando a inspeção for obrigatória, deve existir pelo menos uma tentativa.");
        }

        if (Boolean.FALSE.equals(dto.getExigeInspecao()) && tentativas != null && tentativas != 0) {
            throw new IllegalArgumentException(
                    "Quando a inspeção não for obrigatória, o máximo de tentativas deve ser zero.");
        }
    }

    private void validarRounds(ConfigSumoDTO dto) {
        if (!Integer.valueOf(ROUNDS_REGULARES_RRC).equals(dto.getNumeroRounds())
                || !Integer.valueOf(VITORIAS_PARA_VENCER_RRC).equals(dto.getRoundsParaVencer())) {
            throw new IllegalArgumentException(
                    "O perfil competitivo RRC de Sumô usa 3 rounds regulares e 2 vitórias para vencer.");
        }

        Integer maxExtras = dto.getMaxRoundsExtras();
        if (Boolean.TRUE.equals(dto.getPermiteRoundDesempate())) {
            if (maxExtras == null || maxExtras < 1 || maxExtras > MAX_ROUNDS_EXTRAS_RRC) {
                throw new IllegalArgumentException(
                        "Quando rounds extras estiverem habilitados, o limite deve ficar entre 1 e 2.");
            }
        } else if (maxExtras != null && maxExtras != 0) {
            throw new IllegalArgumentException(
                    "Quando rounds extras estiverem desabilitados, o limite deve ser zero.");
        }
    }

    private void preencherConfig(ConfigSumo config, ConfigSumoDTO dto) {
        config.setPesoMax(dto.getPesoMax());
        config.setExigeInspecao(dto.getExigeInspecao());
        config.setMaxTentativasInspecao(dto.getMaxTentativasInspecao());
        config.setNumeroRounds(dto.getNumeroRounds());
        config.setRoundsParaVencer(dto.getRoundsParaVencer());
        config.setPermiteRoundDesempate(dto.getPermiteRoundDesempate());
        config.setMaxRoundsExtras(Boolean.TRUE.equals(dto.getPermiteRoundDesempate())
                ? dto.getMaxRoundsExtras()
                : 0);
    }
}
