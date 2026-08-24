package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.RegistrationRepository;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionCategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final RobotRepository robotRepository;
    private final CompetitorRepository competitorRepository;
    private final UserAccountService userAccountService;

    @Transactional
    public RegistrationDTO criar(RegistrationDTO dto) {
        return criarInterno(dto, null, false);
    }

    @Transactional
    public RegistrationDTO criarPorParticipante(RegistrationDTO dto, UserAccount solicitante) {
        if (solicitante.getRole() != UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException("Somente PARTICIPANTE pode enviar inscrição por este fluxo.");
        }
        return criarInterno(dto, solicitante, true);
    }

    private RegistrationDTO criarInterno(
            RegistrationDTO dto,
            UserAccount solicitante,
            boolean exigirCompetidor) {

        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        Team team = buscarTeam(dto.getTeamId());
        Robot robot = buscarRobot(dto.getRobotId());

        validarDisponibilidade(competition, category, team, robot);
        validarInscricoesAbertas(competition);
        validarRobotDaEquipe(robot, team);
        validarDuplicidade(dto, null);

        Set<Competitor> competitors = buscarCompetidores(dto.getCompetitorIds(), team, exigirCompetidor);

        Registration registration = new Registration();
        preencher(registration, dto, competition, category, team, robot, competitors);
        registration.setRequestedByUser(solicitante);
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setAtivo(true);
        return new RegistrationDTO(registrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> listar(boolean apenasAtivas) {
        return (apenasAtivas ? registrationRepository.findByAtivoTrueOrderByDataCadastroDesc()
                : registrationRepository.findAllByOrderByDataCadastroDesc())
                .stream().map(RegistrationDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> listarPorEquipe(Long teamId, boolean apenasAtivas) {
        buscarTeam(teamId);
        return (apenasAtivas
                ? registrationRepository.findByTeamIdAndAtivoTrueOrderByDataCadastroDesc(teamId)
                : registrationRepository.findByTeamIdOrderByDataCadastroDesc(teamId))
                .stream().map(RegistrationDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationDTO buscarPorId(Long id) {
        return new RegistrationDTO(buscarRegistration(id));
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> listarPorCompeticao(Long competitionId) {
        buscarCompetition(competitionId);
        return registrationRepository.findByCompetitionIdOrderByDataCadastroDesc(competitionId)
                .stream().map(RegistrationDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> listarPorStatus(StatusRegistration status) {
        return registrationRepository.findByStatusOrderByDataCadastroDesc(status)
                .stream().map(RegistrationDTO::new).toList();
    }

    @Transactional
    public RegistrationDTO atualizar(Long id, RegistrationDTO dto) {
        Registration registration = buscarRegistration(id);
        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        Team team = buscarTeam(dto.getTeamId());
        Robot robot = buscarRobot(dto.getRobotId());

        validarDisponibilidade(competition, category, team, robot);
        validarRobotDaEquipe(robot, team);
        validarDuplicidade(dto, id);

        Set<Competitor> competitors = buscarCompetidores(dto.getCompetitorIds(), team, false);
        preencher(registration, dto, competition, category, team, robot, competitors);

        if (dto.getStatus() != null && dto.getStatus() != registration.getStatus()) {
            aplicarRevisaoSeNecessario(registration, dto.getStatus());
            registration.setStatus(dto.getStatus());
        }
        if (dto.getAtivo() != null) registration.setAtivo(dto.getAtivo());
        return new RegistrationDTO(registrationRepository.save(registration));
    }

    @Transactional
    public void deletar(Long id) {
        Registration registration = buscarRegistration(id);
        registration.setAtivo(false);
        registration.setStatus(StatusRegistration.CANCELADA);
        registrationRepository.save(registration);
    }

    @Transactional
    public RegistrationDTO reativar(Long id) {
        Registration registration = buscarRegistration(id);
        validarDisponibilidade(
                registration.getCompetition(), registration.getCategory(),
                registration.getTeam(), registration.getRobot());
        registration.setAtivo(true);
        registration.setStatus(StatusRegistration.PENDENTE);
        registration.setReviewedByUser(null);
        registration.setReviewedAt(null);
        return new RegistrationDTO(registrationRepository.save(registration));
    }

    private void aplicarRevisaoSeNecessario(Registration registration, StatusRegistration novoStatus) {
        if (novoStatus != StatusRegistration.APROVADA && novoStatus != StatusRegistration.REJEITADA) return;

        UserAccount atual = userAccountService.buscarAtual();
        if (atual.getRole() != UserRole.ORGANIZACAO) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas a ORGANIZAÇÃO pode aprovar ou rejeitar inscrições.");
        }
        registration.setReviewedByUser(atual);
        registration.setReviewedAt(LocalDateTime.now());
    }

    private Set<Competitor> buscarCompetidores(List<Long> ids, Team team, boolean obrigatorio) {
        if (ids == null || ids.isEmpty()) {
            if (obrigatorio) {
                throw new IllegalArgumentException("Informe ao menos um competidor para a inscrição.");
            }
            return new LinkedHashSet<>();
        }

        Set<Long> unicos = new LinkedHashSet<>(ids);
        Set<Competitor> result = new LinkedHashSet<>();
        for (Long id : unicos) {
            Competitor competitor = competitorRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Competidor não encontrado: " + id));
            if (!Boolean.TRUE.equals(competitor.getAtivo())) {
                throw new IllegalArgumentException("Competidor inativo não pode integrar a inscrição: " + id);
            }
            if (!competitor.getTeam().getId().equals(team.getId())) {
                throw new IllegalArgumentException("Todos os competidores da inscrição devem pertencer à equipe informada.");
            }
            result.add(competitor);
        }
        return result;
    }

    private void validarDisponibilidade(Competition c, CompetitionCategory category, Team team, Robot robot) {
        if (!Boolean.TRUE.equals(c.getAtivo())) throw new IllegalArgumentException("Competição inativa.");
        if (!Boolean.TRUE.equals(category.getAtivo())) throw new IllegalArgumentException("Categoria inativa.");
        if (!Boolean.TRUE.equals(team.getAtivo())) throw new IllegalArgumentException("Equipe inativa.");
        if (!Boolean.TRUE.equals(team.getInstitution().getAtivo())) throw new IllegalArgumentException("Instituição inativa.");
        if (!Boolean.TRUE.equals(robot.getAtivo())) throw new IllegalArgumentException("Robô inativo.");
    }

    private void validarInscricoesAbertas(Competition competition) {
        LocalDate hoje = LocalDate.now();
        boolean periodoValido = !hoje.isBefore(competition.getInicioInscricoes())
                && !hoje.isAfter(competition.getFimInscricoes());
        if (competition.getStatus() != StatusCompetition.INSCRICOES_ABERTAS || !periodoValido) {
            throw new IllegalArgumentException("As inscrições não estão abertas para esta competição.");
        }
    }

    private void validarRobotDaEquipe(Robot robot, Team team) {
        if (!robot.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("O robô não pertence à equipe informada.");
        }
    }

    private void validarDuplicidade(RegistrationDTO dto, Long id) {
        boolean existe = id == null
                ? registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(
                        dto.getCompetitionId(), dto.getCategoryId(), dto.getRobotId())
                : registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotIdAndIdNot(
                        dto.getCompetitionId(), dto.getCategoryId(), dto.getRobotId(), id);
        if (existe) throw new IllegalArgumentException("Este robô já está inscrito nesta categoria da competição.");
    }

    private Registration buscarRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id));
    }

    private Competition buscarCompetition(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id));
    }

    private CompetitionCategory buscarCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id));
    }

    private Team buscarTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + id));
    }

    private Robot buscarRobot(Long id) {
        return robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + id));
    }

    private void preencher(
            Registration registration,
            RegistrationDTO dto,
            Competition competition,
            CompetitionCategory category,
            Team team,
            Robot robot,
            Set<Competitor> competitors) {
        registration.setCompetition(competition);
        registration.setCategory(category);
        registration.setTeam(team);
        registration.setRobot(robot);
        registration.setCompetitors(competitors);
        registration.setObservacao(
                dto.getObservacao() == null || dto.getObservacao().isBlank()
                        ? null : dto.getObservacao().trim());
    }
}
