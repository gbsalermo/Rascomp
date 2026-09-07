package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionJudgeDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionJudge;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.repository.CompetitionJudgeRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionJudgeService {

    private final CompetitionJudgeRepository judgeRepository;
    private final CompetitionRepository competitionRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public CompetitionJudgeDTO criar(CompetitionJudgeDTO dto) {
        Competition competition = buscarCompetition(dto.getCompetitionId());
        if (!Boolean.TRUE.equals(competition.getAtivo())) {
            throw new IllegalArgumentException("Não é possível cadastrar juiz em competição inativa.");
        }

        CompetitionJudge judge = new CompetitionJudge();
        judge.setCompetition(competition);
        preencher(judge, dto);
        judge.setAtivo(true);
        return new CompetitionJudgeDTO(judgeRepository.save(judge));
    }

    @Transactional(readOnly = true)
    public List<CompetitionJudgeDTO> listar(Long competitionId, boolean apenasAtivos) {
        buscarCompetition(competitionId);
        return (apenasAtivos
                ? judgeRepository.findByCompetitionIdAndAtivoTrueOrderByNomeAsc(competitionId)
                : judgeRepository.findByCompetitionIdOrderByNomeAsc(competitionId))
                .stream().map(CompetitionJudgeDTO::new).toList();
    }

    @Transactional
    public CompetitionJudgeDTO atualizar(Long id, CompetitionJudgeDTO dto) {
        CompetitionJudge judge = buscarJudge(id);
        if (dto.getCompetitionId() != null && !judge.getCompetition().getId().equals(dto.getCompetitionId())) {
            throw new IllegalArgumentException("O juiz não pode ser transferido entre competições pelo fluxo comum.");
        }
        preencher(judge, dto);
        if (dto.getAtivo() != null) judge.setAtivo(dto.getAtivo());
        return new CompetitionJudgeDTO(judgeRepository.save(judge));
    }

    @Transactional
    public void desativar(Long id) {
        CompetitionJudge judge = buscarJudge(id);
        judge.setAtivo(false);
        judgeRepository.save(judge);
    }

    private void preencher(CompetitionJudge judge, CompetitionJudgeDTO dto) {
        judge.setNome(dto.getNome().trim());
        judge.setUserAccount(buscarUsuarioOpcional(dto.getUserAccountId()));
    }

    private UserAccount buscarUsuarioOpcional(Long id) {
        if (id == null) return null;
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
        if (!Boolean.TRUE.equals(user.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo não pode ser vinculado como juiz.");
        }
        return user;
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionJudge buscarJudge(Long id) {
        return judgeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Juiz não encontrado: " + id));
    }
}
