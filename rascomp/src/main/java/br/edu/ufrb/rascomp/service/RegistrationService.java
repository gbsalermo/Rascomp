package br.edu.ufrb.rascomp.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegistrationDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.CompetitionCategory;
import br.edu.ufrb.rascomp.model.Registration;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.StatusRegistration;
import br.edu.ufrb.rascomp.repository.CompetitionCategoryRepository;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
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

    @Transactional
    public RegistrationDTO criar(RegistrationDTO dto) {
        Competition competition = buscarCompetition(dto.getCompetitionId());
        CompetitionCategory category = buscarCategory(dto.getCategoryId());
        Team team = buscarTeam(dto.getTeamId());
        Robot robot = buscarRobot(dto.getRobotId());

        validarDisponibilidade(competition, category, team, robot);
        validarInscricoesAbertas(competition);
        validarRobotDaEquipe(robot, team);
        validarDuplicidade(dto, null);

        Registration registration = new Registration();
        preencher(registration, dto, competition, category, team, robot);
        registration.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusRegistration.PENDENTE);
        registration.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return new RegistrationDTO(registrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<RegistrationDTO> listar(boolean apenasAtivas) {
        return (apenasAtivas ? registrationRepository.findByAtivoTrueOrderByDataCadastroDesc()
                : registrationRepository.findAllByOrderByDataCadastroDesc())
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
        preencher(registration, dto, competition, category, team, robot);
        if (dto.getStatus() != null) registration.setStatus(dto.getStatus());
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
        validarDisponibilidade(registration.getCompetition(), registration.getCategory(), registration.getTeam(), registration.getRobot());
        registration.setAtivo(true);
        registration.setStatus(StatusRegistration.PENDENTE);
        return new RegistrationDTO(registrationRepository.save(registration));
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
        boolean periodoValido = !hoje.isBefore(competition.getInicioInscricoes()) && !hoje.isAfter(competition.getFimInscricoes());
        if (competition.getStatus() != StatusCompetition.INSCRICOES_ABERTAS || !periodoValido)
            throw new IllegalArgumentException("As inscrições não estão abertas para esta competição.");
    }

    private void validarRobotDaEquipe(Robot robot, Team team) {
        if (!robot.getTeam().getId().equals(team.getId()))
            throw new IllegalArgumentException("O robô não pertence à equipe informada.");
    }

    private void validarDuplicidade(RegistrationDTO dto, Long id) {
        boolean existe = id == null
                ? registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotId(dto.getCompetitionId(), dto.getCategoryId(), dto.getRobotId())
                : registrationRepository.existsByCompetitionIdAndCategoryIdAndRobotIdAndIdNot(dto.getCompetitionId(), dto.getCategoryId(), dto.getRobotId(), id);
        if (existe) throw new IllegalArgumentException("Este robô já está inscrito nesta categoria da competição.");
    }

    private Registration buscarRegistration(Long id) { return registrationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada: " + id)); }
    private Competition buscarCompetition(Long id) { return competitionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Competição não encontrada: " + id)); }
    private CompetitionCategory buscarCategory(Long id) { return categoryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + id)); }
    private Team buscarTeam(Long id) { return teamRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + id)); }
    private Robot buscarRobot(Long id) { return robotRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Robô não encontrado: " + id)); }

    private void preencher(Registration r, RegistrationDTO dto, Competition c, CompetitionCategory category, Team team, Robot robot) {
        r.setCompetition(c);
        r.setCategory(category);
        r.setTeam(team);
        r.setRobot(robot);
        r.setObservacao(dto.getObservacao() == null || dto.getObservacao().isBlank() ? null : dto.getObservacao().trim());
    }
}
