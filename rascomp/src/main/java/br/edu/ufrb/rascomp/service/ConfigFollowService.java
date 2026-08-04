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

	private final ConfigFollowRepository configFollowRepository;
	private final CompetitionCategoryRepository competitionCategoryRepository;
	
	
	@Transactional
	public ConfigFollowDTO criar( Long categoryId, ConfigFollowDTO dto) {
		
		CompetitionCategory category = buscarCategoria(categoryId);
		
		validarCategoriaAtiva(category);
		validarModalidade(category);
		validarConfiguracaoInexistente(categoryId);
		
		ConfigFollow config = new ConfigFollow();
		
		config.setCompetitionCategory(category);
		preencherConfig(config, dto);
		
		ConfigFollow salvo = configFollowRepository.save(config);
		
		return new ConfigFollowDTO(salvo);
	}
	
	
	@Transactional(readOnly = true)
	public ConfigFollowDTO buscarPorCategoria(Long categoryId) {
	
	ConfigFollow config = buscarConfigPorCategoria(categoryId);
	
	return new ConfigFollowDTO(config);
	}

	@Transactional
	public ConfigFollowDTO atualiza(Long categoryId, ConfigFollowDTO dto) {
		
		ConfigFollow config = buscarConfigPorCategoria(categoryId);
		
		CompetitionCategory category = config.getCompetitionCategory();
		
		 validarCategoriaAtiva(category);
	     validarModalidade(category);

	     preencherConfig(config, dto);

	     ConfigFollow atualizado =
	            configFollowRepository.save(config);

	        return new ConfigFollowDTO(atualizado);
	}
	
	@Transactional
	public void deletar(Long categoryId) {
		
		ConfigFollow config =
                buscarConfigPorCategoria(categoryId);

        configFollowRepository.delete(config);
			
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
	
	private ConfigFollow buscarConfigPorCategoria(
            Long categoryId) {

        return configFollowRepository
                .findByCompetitionCategoryId(categoryId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Configuração de Seguidor de Linha "
                                        + "não encontrada para a categoria: "
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

	        if (category.getModalidade() != Modalidade.FOLLOW_LINE){

	            throw new IllegalArgumentException(
	                    "ConfigFollow só pode ser associada "
	                            + "a uma categoria da modalidade "
	                            + "SEGUIDOR_LINHA."
	            );
	        }
	    }

	    private void validarConfiguracaoInexistente(
	            Long categoryId) {

	        if (configFollowRepository
	                .existsByCompetitionCategoryId(categoryId)) {

	            throw new IllegalArgumentException(
	                    "A categoria já possui uma configuração "
	                            + "de Seguidor de Linha."
	            );
	        }
	    }

	    private void preencherConfig(
	            ConfigFollow config,
	            ConfigFollowDTO dto) {

	        config.setNumeroTomadas(
	                dto.getNumeroTomadas()
	        );

	        config.setTentativasPorTomada(
	                dto.getTentativasPorTomada()
	        );

	        config.setMaxTempoSegundos(
	                dto.getMaxTempoSegundos()
	        );

	        config.setNumeroCheckpoints(
	                dto.getNumeroCheckpoints()
	        );
	    }
}
