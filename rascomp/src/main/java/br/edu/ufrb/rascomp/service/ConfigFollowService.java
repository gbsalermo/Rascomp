package br.edu.ufrb.rascomp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.ConfigFollowDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.ConfigFollow;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.ConfigFollowRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfigFollowService {

    public static final int NUMERO_TOMADAS_RRC = 3;
    public static final int TENTATIVAS_POR_TOMADA_RRC = 3;
    public static final int PENALIDADE_PADRAO_SEGUNDOS = 10;
    public static final int TEMPO_APRESENTACAO_PADRAO_SEGUNDOS = 60;

    private final ConfigFollowRepository configFollowRepository;
    private final CompetitionCategoryRepository competitionCategoryRepository;

    @Transactional
    public ConfigFollowDTO criar(Long categoryId, ConfigFollowDTO dto) {
        CompetitionCategory category = buscarCategoria(categoryId);

        validarCategoriaAtiva(category);
        validarModalidade(category);
        validarConfiguracaoInexistente(categoryId);
        validarEstruturaRrc(dto);

        ConfigFollow config = new ConfigFollow();
        config.setCompetitionCategory(category);
        preencherConfig(config, dto);

        return new ConfigFollowDTO(configFollowRepository.save(config));
    }

    @Transactional(readOnly = true)
    public ConfigFollowDTO buscarPorCategoria(Long categoryId) {
        return new ConfigFollowDTO(buscarConfigPorCategoria(categoryId));
    }

    @Transactional
    public ConfigFollowDTO atualiza(Long categoryId, ConfigFollowDTO dto) {
        ConfigFollow config = buscarConfigPorCategoria(categoryId);
        CompetitionCategory category = config.getCompetitionCategory();

        validarCategoriaAtiva(category);
        validarModalidade(category);
        validarEstruturaRrc(dto);
        preencherConfig(config, dto);

        return new ConfigFollowDTO(configFollowRepository.save(config));
    }

    @Transactional
    public void deletar(Long categoryId) {
        configFollowRepository.delete(buscarConfigPorCategoria(categoryId));
    }

    private CompetitionCategory buscarCategoria(Long categoryId) {
        return competitionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoria não encontrada com o id: " + categoryId));
    }

    private ConfigFollow buscarConfigPorCategoria(Long categoryId) {
        return configFollowRepository.findByCompetitionCategoryId(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Configuração de Seguidor de Linha não encontrada para a categoria: " + categoryId));
    }

    private void validarCategoriaAtiva(CompetitionCategory category) {
        if (!Boolean.TRUE.equals(category.getAtivo())) {
            throw new IllegalArgumentException("Não é possível configurar uma categoria inativa.");
        }
    }

    private void validarModalidade(CompetitionCategory category) {
        if (category.getModalidade() != Modalidade.FOLLOW_LINE) {
            throw new IllegalArgumentException(
                    "ConfigFollow só pode ser associada a uma categoria da modalidade FOLLOW_LINE.");
        }
    }

    private void validarConfiguracaoInexistente(Long categoryId) {
        if (configFollowRepository.existsByCompetitionCategoryId(categoryId)) {
            throw new IllegalArgumentException(
                    "A categoria já possui uma configuração de Seguidor de Linha.");
        }
    }

    private void validarEstruturaRrc(ConfigFollowDTO dto) {
        if (!Integer.valueOf(NUMERO_TOMADAS_RRC).equals(dto.getNumeroTomadas())
                || !Integer.valueOf(TENTATIVAS_POR_TOMADA_RRC).equals(dto.getTentativasPorTomada())) {
            throw new IllegalArgumentException(
                    "O Follow Line do RRC usa exatamente 3 tomadas com 3 tentativas por tomada.");
        }
    }

    private void preencherConfig(ConfigFollow config, ConfigFollowDTO dto) {
        config.setNumeroTomadas(NUMERO_TOMADAS_RRC);
        config.setTentativasPorTomada(TENTATIVAS_POR_TOMADA_RRC);
        config.setMaxTempoSegundos(dto.getMaxTempoSegundos());
        config.setNumeroCheckpoints(dto.getNumeroCheckpoints());
        config.setPenalidadePadraoSegundos(dto.getPenalidadePadraoSegundos() != null
                ? dto.getPenalidadePadraoSegundos()
                : PENALIDADE_PADRAO_SEGUNDOS);
        config.setTempoApresentacaoSegundos(dto.getTempoApresentacaoSegundos() != null
                ? dto.getTempoApresentacaoSegundos()
                : TEMPO_APRESENTACAO_PADRAO_SEGUNDOS);
    }
}
