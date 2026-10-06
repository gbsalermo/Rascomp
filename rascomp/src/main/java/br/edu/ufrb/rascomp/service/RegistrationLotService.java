package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationLotDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.RegistrationLot;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.RegistrationLotRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationLotService {

    private final RegistrationLotRepository repository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionContextService competitionContextService;

    @Transactional(readOnly = true)
    public List<RegistrationLotDTO> listar(Long competitionId) {
        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetition(competitionId);
        LocalDate hoje = LocalDate.now();
        return repository.findByCompetitionIdAndAtivoTrueOrderByDataInicioAscIdAsc(competition.getId())
                .stream()
                .map(item -> new RegistrationLotDTO(item, hoje))
                .toList();
    }

    @Transactional
    public RegistrationLotDTO criar(Long competitionId, RegistrationLotDTO dto) {
        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetition(competitionId);
        validarMutavel(competition);
        normalizar(dto);
        validarPeriodo(competition, dto, null);

        RegistrationLot entity = new RegistrationLot();
        preencher(entity, competition, dto);
        entity.setAtivo(true);
        return new RegistrationLotDTO(repository.save(entity), LocalDate.now());
    }

    @Transactional
    public RegistrationLotDTO atualizar(Long competitionId, Long lotId, RegistrationLotDTO dto) {
        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetition(competitionId);
        validarMutavel(competition);

        RegistrationLot entity = buscarLote(lotId);
        exigirDaCompeticao(entity, competitionId);
        normalizar(dto);
        validarPeriodo(competition, dto, entity.getId());

        preencher(entity, competition, dto);
        entity.setAtivo(true);
        return new RegistrationLotDTO(repository.save(entity), LocalDate.now());
    }

    @Transactional
    public void remover(Long competitionId, Long lotId) {
        competitionContextService.exigirOperavel(competitionId);
        Competition competition = buscarCompetition(competitionId);
        validarMutavel(competition);

        RegistrationLot entity = buscarLote(lotId);
        exigirDaCompeticao(entity, competitionId);
        entity.setAtivo(false);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public RegistrationLotDTO buscarAtualPublico(Long competitionId) {
        Competition competition = buscarCompetition(competitionId);
        boolean publico = Boolean.TRUE.equals(competition.getAtivo())
                && (competition.getStatus() == StatusCompetition.INSCRICOES_ABERTAS
                    || competition.getStatus() == StatusCompetition.INSCRICOES_ENCERRADAS
                    || competition.getStatus() == StatusCompetition.EM_ANDAMENTO);

        if (!publico) {
            throw new EntityNotFoundException("Lote público indisponível para a competição: " + competitionId);
        }

        RegistrationLot atual = buscarAtualEntidade(competitionId, LocalDate.now());
        return atual == null ? null : new RegistrationLotDTO(atual, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public RegistrationLot resolverParaNovaInscricao(Competition competition) {
        if (!repository.existsByCompetitionIdAndAtivoTrue(competition.getId())) {
            return null;
        }

        RegistrationLot atual = buscarAtualEntidade(competition.getId(), LocalDate.now());
        if (atual == null) {
            throw new IllegalArgumentException(
                    "Existem lotes configurados, mas nenhum lote de inscrição está vigente hoje.");
        }
        return atual;
    }

    private RegistrationLot buscarAtualEntidade(Long competitionId, LocalDate hoje) {
        return repository
                .findFirstByCompetitionIdAndAtivoTrueAndDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
                        competitionId,
                        hoje,
                        hoje)
                .orElse(null);
    }

    private void validarPeriodo(Competition competition, RegistrationLotDTO dto, Long idIgnorado) {
        if (dto.getDataInicio().isAfter(dto.getDataFim())) {
            throw new IllegalArgumentException("O início do lote não pode ser posterior ao fim.");
        }

        if (dto.getDataInicio().isBefore(competition.getInicioInscricoes())
                || dto.getDataFim().isAfter(competition.getFimInscricoes())) {
            throw new IllegalArgumentException(
                    "O lote deve ficar integralmente dentro do período geral de inscrições.");
        }

        List<RegistrationLot> lotes = repository
                .findByCompetitionIdAndAtivoTrueOrderByDataInicioAscIdAsc(competition.getId());

        boolean sobrepoe = lotes.stream()
                .filter(item -> idIgnorado == null || !item.getId().equals(idIgnorado))
                .anyMatch(item ->
                        !dto.getDataFim().isBefore(item.getDataInicio())
                                && !dto.getDataInicio().isAfter(item.getDataFim()));

        if (sobrepoe) {
            throw new IllegalArgumentException("O período informado se sobrepõe a outro lote ativo.");
        }

        boolean nomeDuplicado = lotes.stream()
                .filter(item -> idIgnorado == null || !item.getId().equals(idIgnorado))
                .anyMatch(item -> item.getNome().equalsIgnoreCase(dto.getNome()));

        if (nomeDuplicado) {
            throw new IllegalArgumentException("Já existe outro lote ativo com esse nome nesta competição.");
        }
    }

    private void validarMutavel(Competition competition) {
        if (competition.getStatus() != StatusCompetition.PLANEJADA
                && competition.getStatus() != StatusCompetition.INSCRICOES_ABERTAS) {
            throw new IllegalArgumentException(
                    "Lotes só podem ser configurados antes da competição ou enquanto as inscrições estiverem abertas.");
        }
    }

    private void normalizar(RegistrationLotDTO dto) {
        if (dto.getNome() != null) dto.setNome(dto.getNome().trim());
    }

    private void preencher(RegistrationLot entity, Competition competition, RegistrationLotDTO dto) {
        entity.setCompetition(competition);
        entity.setNome(dto.getNome());
        entity.setDataInicio(dto.getDataInicio());
        entity.setDataFim(dto.getDataFim());
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competição não encontrada com o id: " + id));
    }

    private RegistrationLot buscarLote(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lote de inscrição não encontrado com o id: " + id));
    }

    private void exigirDaCompeticao(RegistrationLot lot, Long competitionId) {
        if (!lot.getCompetition().getId().equals(competitionId)) {
            throw new IllegalArgumentException("O lote informado não pertence a esta competição.");
        }
    }
}
