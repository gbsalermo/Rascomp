package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public TeamDTO criar(TeamDTO dto) {
        normalizar(dto);
        Institution institution = buscarInstituicao(dto.getInstitutionId());
        validarInstituicaoAtiva(institution);
        validarNomeDuplicado(dto.getNome(), institution.getId());

        UserAccount responsavel = dto.getResponsibleUserId() == null
                ? null
                : buscarResponsavel(dto.getResponsibleUserId());

        Team team = new Team();
        preencherTeam(team, dto, institution, responsavel);
        team.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new TeamDTO(teamRepository.save(team));
    }

    @Transactional
    public TeamDTO criarParaResponsavel(TeamDTO dto, UserAccount responsavel) {
        validarResponsavel(responsavel);
        dto.setResponsibleUserId(responsavel.getId());
        dto.setAtivo(true);
        return criar(dto);
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> listarTodos() {
        return teamRepository.findAllByOrderByNomeAsc().stream().map(TeamDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> listarAtivas() {
        return teamRepository.findByAtivoTrueOrderByNomeAsc().stream().map(TeamDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> listarPorResponsavel(Long userId) {
        return teamRepository.findByResponsibleUserIdAndAtivoTrueOrderByNomeAsc(userId)
                .stream().map(TeamDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public TeamDTO buscarPorId(Long id) {
        return new TeamDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> listarPorInstituicao(Long institutionId, boolean apenasAtivas) {
        buscarInstituicao(institutionId);
        List<Team> teams = apenasAtivas
                ? teamRepository.findByInstitutionIdAndAtivoTrueOrderByNomeAsc(institutionId)
                : teamRepository.findByInstitutionIdOrderByNomeAsc(institutionId);
        return teams.stream().map(TeamDTO::new).toList();
    }

    @Transactional
    public TeamDTO atualizar(Long id, TeamDTO dto) {
        Team team = buscarEntidade(id);
        normalizar(dto);

        Institution institution = buscarInstituicao(dto.getInstitutionId());
        validarInstituicaoAtiva(institution);
        validarNomeDuplicadoNaAtualizacao(dto.getNome(), institution.getId(), id);

        UserAccount responsavel = dto.getResponsibleUserId() == null
                ? null
                : buscarResponsavel(dto.getResponsibleUserId());

        preencherTeam(team, dto, institution, responsavel);
        if (dto.getAtivo() != null) team.setAtivo(dto.getAtivo());
        return new TeamDTO(teamRepository.save(team));
    }

    @Transactional
    public TeamDTO atualizarComoResponsavel(Long id, TeamDTO dto, UserAccount responsavel) {
        Team team = buscarEntidade(id);
        if (team.getResponsibleUser() == null
                || !team.getResponsibleUser().getId().equals(responsavel.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não é o responsável por esta equipe.");
        }
        dto.setResponsibleUserId(responsavel.getId());
        dto.setAtivo(team.getAtivo());
        return atualizar(id, dto);
    }

    @Transactional
    public void deletar(Long id) {
        Team team = buscarEntidade(id);
        team.setAtivo(false);
        teamRepository.save(team);
    }

    @Transactional
    public TeamDTO reativar(Long id) {
        Team team = buscarEntidade(id);
        validarInstituicaoAtiva(team.getInstitution());
        team.setAtivo(true);
        return new TeamDTO(teamRepository.save(team));
    }

    private Team buscarEntidade(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada com o id: " + id));
    }

    private Institution buscarInstituicao(Long institutionId) {
        return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Instituição não encontrada com o id: " + institutionId));
    }

    private UserAccount buscarResponsavel(Long id) {
        UserAccount usuario = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário responsável não encontrado: " + id));
        validarResponsavel(usuario);
        return usuario;
    }

    private void validarResponsavel(UserAccount usuario) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("O responsável da equipe deve possuir conta ativa.");
        }
        if (usuario.getRole() != UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException("O responsável da equipe deve ser um usuário PARTICIPANTE.");
        }
    }

    private void validarInstituicaoAtiva(Institution institution) {
        if (!Boolean.TRUE.equals(institution.getAtivo())) {
            throw new IllegalArgumentException("Não é possível vincular a equipe a uma instituição inativa.");
        }
    }

    private void validarNomeDuplicado(String nome, Long institutionId) {
        if (teamRepository.existsByNomeIgnoreCaseAndInstitutionId(nome, institutionId)) {
            throw new IllegalArgumentException(
                    "A instituição já possui uma equipe cadastrada com o nome: " + nome);
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(String nome, Long institutionId, Long teamId) {
        if (teamRepository.existsByNomeIgnoreCaseAndInstitutionIdAndIdNot(nome, institutionId, teamId)) {
            throw new IllegalArgumentException(
                    "A instituição já possui outra equipe cadastrada com o nome: " + nome);
        }
    }

    private void normalizar(TeamDTO dto) {
        dto.setNome(dto.getNome().trim());
    }

    private void preencherTeam(Team team, TeamDTO dto, Institution institution, UserAccount responsavel) {
        team.setNome(dto.getNome());
        team.setInstitution(institution);
        team.setResponsibleUser(responsavel);
    }
}
