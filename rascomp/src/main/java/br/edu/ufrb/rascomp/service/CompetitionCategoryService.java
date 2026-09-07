package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionCategoryDTO;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionCategoryService {

    private final CompetitionCategoryRepository competitionCategoryRepository;

    @Transactional
    public CompetitionCategoryDTO criar(CompetitionCategoryDTO dto) {
        CompetitionCategory category = new CompetitionCategory();
        preencherCategory(category, dto);
        CompetitionCategory salvo = competitionCategoryRepository.save(category);
        return new CompetitionCategoryDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<CompetitionCategoryDTO> listarTodos() {
        return competitionCategoryRepository.findAll()
                .stream()
                .map(CompetitionCategoryDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitionCategoryDTO buscarPorId(Long id) {
        CompetitionCategory category = competitionCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com o id: " + id));
        return new CompetitionCategoryDTO(category);
    }

    @Transactional(readOnly = true)
    public List<CompetitionCategoryDTO> listarPorModalidade(Modalidade modalidade) {
        return competitionCategoryRepository.findByModalidade(modalidade)
                .stream()
                .map(CompetitionCategoryDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitionCategoryDTO> listarPorModalidadeAtiva(Modalidade modalidade) {
        return competitionCategoryRepository.findByModalidadeAndAtivoTrue(modalidade)
                .stream()
                .map(CompetitionCategoryDTO::new)
                .toList();
    }

    @Transactional
    public CompetitionCategoryDTO atualizar(Long id, CompetitionCategoryDTO dto) {
        CompetitionCategory category = competitionCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));
        preencherCategory(category, dto);
        CompetitionCategory atualizado = competitionCategoryRepository.save(category);
        return new CompetitionCategoryDTO(atualizado);
    }

    @Transactional
    public void deletar(Long id) {
        CompetitionCategory category = competitionCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada com o id: " + id));
        category.setAtivo(false);
    }

    private void preencherCategory(CompetitionCategory category, CompetitionCategoryDTO dto) {
        if (dto.getModalidade() == Modalidade.SUMO) {
            if (dto.getSumoPhysicalClass() == null) {
                throw new IllegalArgumentException(
                        "Categoria de Sumô deve informar a classe física: MINI_500G ou SUMO_3KG.");
            }
            if (dto.getSumoControlMode() == null) {
                throw new IllegalArgumentException(
                        "Categoria de Sumô deve informar o modo de controle: AUTONOMO ou RC.");
            }
        }

        category.setNome(dto.getNome().trim());
        category.setDescricao(dto.getDescricao() == null || dto.getDescricao().isBlank()
                ? null : dto.getDescricao().trim());
        category.setModalidade(dto.getModalidade());
        category.setSumoPhysicalClass(dto.getModalidade() == Modalidade.SUMO ? dto.getSumoPhysicalClass() : null);
        category.setSumoControlMode(dto.getModalidade() == Modalidade.SUMO ? dto.getSumoControlMode() : null);
        category.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
    }
}
