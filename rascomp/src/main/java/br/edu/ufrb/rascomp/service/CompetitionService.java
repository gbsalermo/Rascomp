package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;

    @Transactional
    public CompetitionDTO criar(CompetitionDTO dto) {
        normalizar(dto);
        validarDatas(dto);
        validarNomeDuplicado(dto.getNome(), null);
        validarStatusInicial(dto.getStatus());

        Competition competition = new Competition();
        preencher(competition, dto);
        competition.setStatus(StatusCompetition.PLANEJADA);
        competition.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        return new CompetitionDTO(competitionRepository.save(competition));
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTO> listar(boolean apenasAtivas) {
        return (apenasAtivas
                ? competitionRepository.findByAtivoTrueOrderByDataInicioDesc()
                : competitionRepository.findAllByOrderByDataInicioDesc())
                .stream().map(CompetitionDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitionDTO> listarPorStatus(StatusCompetition status) {
        return competitionRepository.findByStatusOrderByDataInicioDesc(status)
                .stream().map(CompetitionDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public CompetitionDTO buscarPorId(Long id) {
        return new CompetitionDTO(buscarCompetition(id));
    }

    @Transactional
    public CompetitionDTO atualizar(Long id, CompetitionDTO dto) {
        Competition competition = buscarCompetition(id);
        normalizar(dto);
        validarDatas(dto);
        validarNomeDuplicado(dto.getNome(), id);

        StatusCompetition novoStatus = dto.getStatus() != null ? dto.getStatus() : competition.getStatus();
        validarTransicaoStatus(competition.getStatus(), novoStatus);
        validarAtivoNoFluxoComum(dto.getAtivo());

        preencher(competition, dto);
        competition.setStatus(novoStatus);
        if (dto.getAtivo() != null) competition.setAtivo(dto.getAtivo());
        return new CompetitionDTO(competitionRepository.save(competition));
    }

    @Transactional
    public void deletar(Long id) {
        Competition competition = buscarCompetition(id);
        competition.setAtivo(false);
        competitionRepository.save(competition);
    }

    @Transactional
    public CompetitionDTO reativar(Long id) {
        Competition competition = buscarCompetition(id);
        competition.setAtivo(true);
        return new CompetitionDTO(competitionRepository.save(competition));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com o id: " + id));
    }

    private void validarStatusInicial(StatusCompetition status) {
        if (status != null && status != StatusCompetition.PLANEJADA) {
            throw new IllegalArgumentException(
                    "Nova competição deve iniciar como PLANEJADA. Use as transições de estado após a criação.");
        }
    }

    private void validarTransicaoStatus(StatusCompetition atual, StatusCompetition novo) {
        if (atual == novo) return;

        boolean permitida = switch (atual) {
            case PLANEJADA -> novo == StatusCompetition.INSCRICOES_ABERTAS
                    || novo == StatusCompetition.CANCELADA;
            case INSCRICOES_ABERTAS -> novo == StatusCompetition.INSCRICOES_ENCERRADAS
                    || novo == StatusCompetition.CANCELADA;
            case INSCRICOES_ENCERRADAS -> novo == StatusCompetition.EM_ANDAMENTO
                    || novo == StatusCompetition.CANCELADA;
            case EM_ANDAMENTO -> novo == StatusCompetition.FINALIZADA
                    || novo == StatusCompetition.CANCELADA;
            case FINALIZADA, CANCELADA -> false;
        };

        if (!permitida) {
            throw new IllegalArgumentException(
                    "Transição de competição inválida: " + atual + " → " + novo
                            + ". Reabertura/prorrogação deve usar operação explícita quando disponível.");
        }
    }

    private void validarAtivoNoFluxoComum(Boolean ativo) {
        if (Boolean.FALSE.equals(ativo)) {
            throw new IllegalArgumentException(
                    "Use a operação específica de desativação para inativar uma competição.");
        }
    }

    private void validarNomeDuplicado(String nome, Long id) {
        boolean existe = id == null
                ? competitionRepository.existsByNomeIgnoreCase(nome)
                : competitionRepository.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (existe) throw new IllegalArgumentException("Já existe uma competição com o nome: " + nome);
    }

    private void validarDatas(CompetitionDTO dto) {
        if (dto.getInicioInscricoes().isAfter(dto.getFimInscricoes()))
            throw new IllegalArgumentException("O início das inscrições não pode ser posterior ao fim.");
        if (dto.getDataInicio().isAfter(dto.getDataFim()))
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        if (dto.getFimInscricoes().isAfter(dto.getDataInicio()))
            throw new IllegalArgumentException("As inscrições devem terminar até o início da competição.");
    }

    private void normalizar(CompetitionDTO dto) {
        dto.setNome(dto.getNome().trim());
        if (dto.getDescricao() != null) {
            String descricao = dto.getDescricao().trim();
            dto.setDescricao(descricao.isBlank() ? null : descricao);
        }
    }

    private void preencher(Competition entity, CompetitionDTO dto) {
        entity.setNome(dto.getNome());
        entity.setDescricao(dto.getDescricao());
        entity.setInicioInscricoes(dto.getInicioInscricoes());
        entity.setFimInscricoes(dto.getFimInscricoes());
        entity.setDataInicio(dto.getDataInicio());
        entity.setDataFim(dto.getDataFim());
    }
}
