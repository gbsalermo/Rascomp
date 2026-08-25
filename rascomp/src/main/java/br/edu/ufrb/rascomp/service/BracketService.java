package br.edu.ufrb.rascomp.service;

import java.util.List;
import java.util.Objects;

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
        Competition competition = buscarCompetitionParaAtualizacao(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        validarAtivos(competition, category);
        validarCategoriaSumo(category);

        Bracket bracket = new Bracket();
        preencher(bracket, dto, competition, category);
        bracket.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusBracket.RASCUNHO);
        bracket.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        tornarAtual(bracket, competition.getId(), category.getId());
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

    @Transactional(readOnly = true)
    public List<BracketDTO> listarAtuaisPorCompeticao(Long competitionId) {
        buscarCompetition(competitionId);
        return bracketRepository
                .findByCompetitionIdAndAtualTrueAndAtivoTrueOrderByDataCadastroDesc(competitionId)
                .stream().map(BracketDTO::new).toList();
    }

    @Transactional
    public BracketDTO atualizar(Long id, BracketDTO dto) {
        Bracket bracket = buscarBracket(id);
        Competition competition = buscarCompetitionParaAtualizacao(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        validarAtivos(competition, category);
        validarCategoriaSumo(category);

        preencher(bracket, dto, competition, category);
        if (dto.getStatus() != null) bracket.setStatus(dto.getStatus());
        if (dto.getAtivo() != null) bracket.setAtivo(dto.getAtivo());

        boolean deveSerAtual = dto.getAtual() != null
                ? dto.getAtual()
                : Boolean.TRUE.equals(bracket.getAtual());
        if (deveSerAtual) tornarAtual(bracket, competition.getId(), category.getId());
        else bracket.setAtual(false);

        return new BracketDTO(bracketRepository.save(bracket));
    }

    @Transactional
    public void deletar(Long id) {
        Bracket bracket = buscarBracket(id);
        bracket.setAtivo(false);
        bracket.setAtual(false);
        bracket.setStatus(StatusBracket.CANCELADO);
        bracketRepository.save(bracket);
    }

    @Transactional
    public BracketDTO reativar(Long id) {
        Bracket bracket = buscarBracket(id);
        Competition competition = buscarCompetitionParaAtualizacao(bracket.getCompetition().getId());
        validarAtivos(competition, bracket.getCategory());
        validarCategoriaSumo(bracket.getCategory());
        bracket.setAtivo(true);
        bracket.setStatus(StatusBracket.RASCUNHO);
        tornarAtual(bracket, competition.getId(), bracket.getCategory().getId());
        return new BracketDTO(bracketRepository.save(bracket));
    }

    private void tornarAtual(Bracket target, Long competitionId, Long categoryId) {
        List<Bracket> atuais = bracketRepository
                .findByCompetitionIdAndCategoryIdAndAtualTrue(competitionId, categoryId);

        List<Bracket> anteriores = atuais.stream()
                .filter(item -> !Objects.equals(item.getId(), target.getId()))
                .peek(item -> item.setAtual(false))
                .toList();

        if (!anteriores.isEmpty()) bracketRepository.saveAll(anteriores);
        target.setAtual(true);
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

    private Bracket buscarBracket(Long id) {
        return bracketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chaveamento não encontrado: " + id));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private Competition buscarCompetitionParaAtualizacao(Long id) {
        return competitionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }

    private void preencher(Bracket entity, BracketDTO dto, Competition competition, CompetitionCategory category) {
        entity.setCompetition(competition);
        entity.setCategory(category);
        entity.setNome(dto.getNome().trim());
    }
}
