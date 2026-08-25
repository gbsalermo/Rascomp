package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitorDTO;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitorService {

    private final CompetitorRepository competitorRepository;
    private final TeamRepository teamRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public CompetitorDTO criar(CompetitorDTO dto) {
        normalizar(dto);
        Team team = buscarEquipe(dto.getTeamId());
        validarEquipeAtiva(team);
        validarInstituicaoAtiva(team.getInstitution());
        validarEmailDuplicado(dto.getEmail());

        UserAccount userAccount = buscarContaOpcional(dto.getUserAccountId(), null, dto.getEmail());

        Competitor competitor = new Competitor();
        preencherCompetitor(competitor, dto, team, userAccount);
        competitor.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new CompetitorDTO(competitorRepository.save(competitor));
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> listarTodos() {
        return competitorRepository.findAllByOrderByNomeAsc().stream().map(CompetitorDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> listarAtivos() {
        return competitorRepository.findByAtivoTrueOrderByNomeAsc().stream().map(CompetitorDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public CompetitorDTO buscarPorId(Long id) {
        return new CompetitorDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public CompetitorDTO buscarPorEmail(String email) {
        String normalizado = email.trim().toLowerCase();
        Competitor competitor = competitorRepository.findByEmailIgnoreCase(normalizado)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competidor não encontrado com o e-mail: " + email));
        return new CompetitorDTO(competitor);
    }

    @Transactional(readOnly = true)
    public List<CompetitorDTO> listarPorEquipe(Long teamId, boolean apenasAtivos) {
        buscarEquipe(teamId);
        List<Competitor> competitors = apenasAtivos
                ? competitorRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId)
                : competitorRepository.findByTeamIdOrderByNomeAsc(teamId);
        return competitors.stream().map(CompetitorDTO::new).toList();
    }

    @Transactional
    public CompetitorDTO atualizar(Long id, CompetitorDTO dto) {
        Competitor competitor = buscarEntidade(id);
        normalizar(dto);
        Team team = buscarEquipe(dto.getTeamId());
        validarEquipeAtiva(team);
        validarInstituicaoAtiva(team.getInstitution());
        validarEmailDuplicadoNaAtualizacao(dto.getEmail(), id);

        UserAccount userAccount = buscarContaOpcional(dto.getUserAccountId(), id, dto.getEmail());
        preencherCompetitor(competitor, dto, team, userAccount);
        if (dto.getAtivo() != null) competitor.setAtivo(dto.getAtivo());
        return new CompetitorDTO(competitorRepository.save(competitor));
    }

    @Transactional
    public void deletar(Long id) {
        Competitor competitor = buscarEntidade(id);
        competitor.setAtivo(false);
        competitorRepository.save(competitor);
    }

    @Transactional
    public CompetitorDTO reativar(Long id) {
        Competitor competitor = buscarEntidade(id);
        validarEquipeAtiva(competitor.getTeam());
        validarInstituicaoAtiva(competitor.getTeam().getInstitution());
        competitor.setAtivo(true);
        return new CompetitorDTO(competitorRepository.save(competitor));
    }

    private Competitor buscarEntidade(Long id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competidor não encontrado com o id: " + id));
    }

    private Team buscarEquipe(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Equipe não encontrada com o id: " + teamId));
    }

    private UserAccount buscarContaOpcional(Long userAccountId, Long competitorId, String emailCompetidor) {
        if (userAccountId == null) return null;

        UserAccount usuario = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de usuário não encontrada: " + userAccountId));

        if (usuario.getRole() != UserRole.PARTICIPANTE || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Somente uma conta PARTICIPANTE ativa pode ser vinculada ao competidor.");
        }

        boolean jaVinculado = competitorId == null
                ? competitorRepository.existsByUserAccountId(userAccountId)
                : competitorRepository.existsByUserAccountIdAndIdNot(userAccountId, competitorId);
        if (jaVinculado) {
            throw new IllegalArgumentException("Esta conta já está vinculada a outro competidor.");
        }

        if (!usuario.getEmail().equalsIgnoreCase(emailCompetidor)) {
            throw new IllegalArgumentException("O e-mail do competidor deve ser o mesmo da conta vinculada.");
        }
        return usuario;
    }

    private void validarEquipeAtiva(Team team) {
        if (!Boolean.TRUE.equals(team.getAtivo())) {
            throw new IllegalArgumentException("Não é possível vincular o competidor a uma equipe inativa.");
        }
    }

    private void validarInstituicaoAtiva(Institution institution) {
        if (!Boolean.TRUE.equals(institution.getAtivo())) {
            throw new IllegalArgumentException("Não é possível vincular o competidor a uma instituição inativa.");
        }
    }

    private void validarEmailDuplicado(String email) {
        if (competitorRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe um competidor cadastrado com o e-mail: " + email);
        }
    }

    private void validarEmailDuplicadoNaAtualizacao(String email, Long id) {
        if (competitorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("Já existe outro competidor cadastrado com o e-mail: " + email);
        }
    }

    private void normalizar(CompetitorDTO dto) {
        dto.setNome(dto.getNome().trim());
        dto.setEmail(dto.getEmail().trim().toLowerCase());
        if (dto.getTelefone() != null) {
            String telefone = dto.getTelefone().trim();
            dto.setTelefone(telefone.isBlank() ? null : telefone);
        }
    }

    private void preencherCompetitor(
            Competitor competitor,
            CompetitorDTO dto,
            Team team,
            UserAccount userAccount) {
        competitor.setNome(dto.getNome());
        competitor.setEmail(dto.getEmail());
        competitor.setTelefone(dto.getTelefone());
        competitor.setTeam(team);
        competitor.setUserAccount(userAccount);
    }
}
