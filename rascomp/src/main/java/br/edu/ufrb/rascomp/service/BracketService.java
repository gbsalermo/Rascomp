package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.BracketDTO;
import br.edu.ufrb.rascomp.model.Bracket;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Enum.Modalidade;
import br.edu.ufrb.rascomp.model.Enum.StatusBracket;
import br.edu.ufrb.rascomp.repository.BracketRepository;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BracketService {
    private final BracketRepository bracketRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;

    @Transactional
    public BracketDTO criar(BracketDTO dto) {
        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        validarAtivos(competition, category);
        validarCategoriaSumo(category);
        validarDuplicidade(dto, null);

        Bracket bracket = new Bracket();
        preencher(bracket, dto, competition, category);
        bracket.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusBracket.RASCUNHO);
        bracket.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new BracketDTO(bracketRepository.save(bracket));
    }

    @Transactional(readOnly = true)
    public List<BracketDTO> listar(boolean apenasAtivos) {
        return (apenasAtivos ? bracketRepository.findByAtivoTrueOrderByDataCadastroDesc()
                : bracketRepository.findAllByOrderByDataCadastroDesc())
                .stream().map(BracketDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public BracketDTO buscarPorId(Long id) {
        return new BracketDTO(buscarBracket(id));
    }

    @Transactional(readOnly = true)
    public List<BracketDTO> listarPorCompeticao(Long competitionId) {
        buscarCompetition(competitionId);
        return bracketRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream().map(BracketDTO::new).toList();
    }

    @Transactional
    public BracketDTO atualizar(Long id, BracketDTO dto) {
        Bracket bracket = buscarBracket(id);
        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        validarAtivos(competition, category);
        validarCategoriaSumo(category);
        validarDuplicidade(dto, id);
        preencher(bracket, dto, competition, category);
        if (dto.getStatus() != null) bracket.setStatus(dto.getStatus());
        if (dto.getAtivo() != null) bracket.setAtivo(dto.getAtivo());
        return new BracketDTO(bracketRepository.save(bracket));
    }

    @Transactional
    public void deletar(Long id) {
        Bracket bracket = buscarBracket(id);
        bracket.setAtivo(false);
        bracket.setStatus(StatusBracket.CANCELADO);
        bracketRepository.save(bracket);
    }

    @Transactional
    public BracketDTO reativar(Long id) {
        Bracket bracket = buscarBracket(id);
        validarAtivos(bracket.getCompetition(), bracket.getCategory());
        validarCategoriaSumo(bracket.getCategory());
        bracket.setAtivo(true);
        bracket.setStatus(StatusBracket.RASCUNHO);
        return new BracketDTO(bracketRepository.save(bracket));
    }

    private void validarAtivos(Competition competition, CompetitionCategory category) {
        if (!Boolean.TRUE.equals(competition.getAtivo())) throw new IllegalArgumentException("Competição inativa.");
        if (!Boolean.TRUE.equals(category.getAtivo())) throw new IllegalArgumentException("Categoria inativa.");
    }

    private void validarCategoriaSumo(CompetitionCategory category) {
        if (category.getModalidade() != Modalidade.SUMO) {
            throw new IllegalArgumentException(
                    "Chaveamento é exclusivo da modalidade SUMO. FOLLOW_LINE é definido pelo ranking de tempos.");
        }
    }

    private void validarDuplicidade(BracketDTO dto, Long id) {
        boolean existe = id == null
                ? bracketRepository.existsByCompetitionIdAndCategoryId(dto.getCompetitionId(), dto.getCategoryId())
                : bracketRepository.existsByCompetitionIdAndCategoryIdAndIdNot(dto.getCompetitionId(), dto.getCategoryId(), id);
        if (existe) throw new IllegalArgumentException("Já existe um chaveamento para esta competição e categoria.");
    }

    private Bracket buscarBracket(Long id) { return bracketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + id)); }
    private Competition buscarCompetition(Long id) { return competitionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id)); }
    private CompetitionCategory buscarCategory(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id)); }

    private void preencher(Bracket entity, BracketDTO dto, Competition competition, CompetitionCategory category) {
        entity.setCompetition(competition);
        entity.setCategory(category);
        entity.setNome(dto.getNome().trim());
    }
}
