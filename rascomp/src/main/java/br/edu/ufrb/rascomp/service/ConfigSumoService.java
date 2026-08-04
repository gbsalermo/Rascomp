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
		
		ConfigSumo salvo = configSumoRepository.save(config);
		
		return new ConfigSumoDTO(salvo);
		
	}
	
	@Transactional(readOnly = true)
	public ConfigSumoDTO buscarPorCategoria(Long categoryId) {
		
		ConfigSumo config = buscarConfigPorCategoria(categoryId);
		
		return new ConfigSumoDTO(config);
		
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
        
        ConfigSumo atualizado = configSumoRepository.save(config);
        
        return new ConfigSumoDTO(atualizado);    	
	}
	
	
	@Transactional
	public void deletar(Long categoryId) {
		
		ConfigSumo config = buscarConfigPorCategoria(categoryId);
		
		configSumoRepository.delete(config);
	}
	
	
	private CompetitionCategory buscarCategoria(
            Long categoryId) {

        return competitionCategoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Categoria não encontrada com o id: "
                                        + categoryId
                        )
                );
    }

    private ConfigSumo buscarConfigPorCategoria(
            Long categoryId) {

        return configSumoRepository
                .findByCategoryId(categoryId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Configuração de Sumô não encontrada "
                                        + "para a categoria com o id: "
                                        + categoryId
                        )
                );
    }

    private void validarCategoriaAtiva(
            CompetitionCategory category) {

        if (!Boolean.TRUE.equals(category.getAtivo())) {
            throw new IllegalArgumentException(
                    "Não é possível configurar uma categoria inativa."
            );
        }
    }

    private void validarModalidade(
            CompetitionCategory category) {

        if (category.getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException(
                    "ConfigSumo só pode ser associada "
                            + "a uma categoria da modalidade SUMO."
            );
        }
    }

    private void validarConfigInexistente(
            Long categoryId) {

        if (configSumoRepository
                .existsByCategoryId(categoryId)) {

            throw new IllegalArgumentException(
                    "A categoria já possui "
                            + "uma configuração de Sumô."
            );
        }
    }

    private void validarInspecao(ConfigSumoDTO dto) {

        Integer tentativas =
                dto.getMaxTentativasInspecao();

        if (Boolean.TRUE.equals(dto.getExigeInspecao())
                && tentativas != null
                && tentativas < 1) {

            throw new IllegalArgumentException(
                    "Quando a inspeção for obrigatória, "
                            + "deve existir pelo menos uma tentativa."
            );
        }

        if (Boolean.FALSE.equals(dto.getExigeInspecao())
                && tentativas != null
                && tentativas != 0) {

            throw new IllegalArgumentException(
                    "Quando a inspeção não for obrigatória, "
                            + "o máximo de tentativas deve ser zero."
            );
        }
    }

    private void validarRounds(ConfigSumoDTO dto) {

        Integer numeroRounds =
                dto.getNumeroRounds();

        Integer roundsParaVencer =
                dto.getRoundsParaVencer();

        if (numeroRounds == null
                || roundsParaVencer == null) {
            return;
        }

        if (numeroRounds % 2 == 0) {
            throw new IllegalArgumentException(
                    "O número de rounds regulares deve ser ímpar."
            );
        }

        int maioriaNecessaria =
                (numeroRounds / 2) + 1;

        if (roundsParaVencer != maioriaNecessaria) {
            throw new IllegalArgumentException(
                    "Para " + numeroRounds
                            + " rounds regulares, são necessárias "
                            + maioriaNecessaria
                            + " vitórias para vencer a batalha."
            );
        }
    }

    private void preencherConfig(
            ConfigSumo config,
            ConfigSumoDTO dto) {

        config.setPesoMax(
                dto.getPesoMax());

        config.setExigeInspecao(
                dto.getExigeInspecao());

        config.setMaxTentativasInspecao(
                dto.getMaxTentativasInspecao());

        config.setNumeroRounds(
                dto.getNumeroRounds());

        config.setRoundsParaVencer(
                dto.getRoundsParaVencer());

        config.setPermiteRoundDesempate(
                dto.getPermiteRoundDesempate());
    }
	

}
