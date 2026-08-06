package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RobotDTO;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Robot;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.repository.RobotRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository robotRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public RobotDTO criar(RobotDTO dto) {
        normalizar(dto);

        Team team = buscarEquipe(dto.getTeamId());

        validarEquipeAtiva(team);
        validarInstituicaoAtiva(team.getInstitution());
        validarNomeDuplicado(dto.getNome(), team.getId());

        Robot robot = new Robot();
        preencherRobot(robot, dto, team);

        robot.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        Robot salvo = robotRepository.save(robot);

        return new RobotDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<RobotDTO> listarTodos() {
        return robotRepository
                .findAllByOrderByNomeAsc()
                .stream()
                .map(RobotDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RobotDTO> listarAtivos() {
        return robotRepository
                .findByAtivoTrueOrderByNomeAsc()
                .stream()
                .map(RobotDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public RobotDTO buscarPorId(Long id) {
        return new RobotDTO(buscarRobot(id));
    }

    @Transactional(readOnly = true)
    public List<RobotDTO> listarPorEquipe(Long teamId, boolean apenasAtivos) {
        buscarEquipe(teamId);

        List<Robot> robots = apenasAtivos
                ? robotRepository.findByTeamIdAndAtivoTrueOrderByNomeAsc(teamId)
                : robotRepository.findByTeamIdOrderByNomeAsc(teamId);

        return robots
                .stream()
                .map(RobotDTO::new)
                .toList();
    }

    @Transactional
    public RobotDTO atualizar(Long id, RobotDTO dto) {
        Robot robot = buscarRobot(id);

        normalizar(dto);

        Team team = buscarEquipe(dto.getTeamId());

        validarEquipeAtiva(team);
        validarInstituicaoAtiva(team.getInstitution());
        validarNomeDuplicadoNaAtualizacao(dto.getNome(), team.getId(), id);

        preencherRobot(robot, dto, team);

        if (dto.getAtivo() != null) {
            robot.setAtivo(dto.getAtivo());
        }

        Robot atualizado = robotRepository.save(robot);

        return new RobotDTO(atualizado);
    }

    @Transactional
    public void deletar(Long id) {
        Robot robot = buscarRobot(id);
        robot.setAtivo(false);
        robotRepository.save(robot);
    }

    @Transactional
    public RobotDTO reativar(Long id) {
        Robot robot = buscarRobot(id);

        validarEquipeAtiva(robot.getTeam());
        validarInstituicaoAtiva(robot.getTeam().getInstitution());

        robot.setAtivo(true);

        Robot reativado = robotRepository.save(robot);

        return new RobotDTO(reativado);
    }

    private Robot buscarRobot(Long id) {
        return robotRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Robô não encontrado com o id: " + id
                ));
    }

    private Team buscarEquipe(Long teamId) {
        return teamRepository
                .findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Equipe não encontrada com o id: " + teamId
                ));
    }

    private void validarEquipeAtiva(Team team) {
        if (!Boolean.TRUE.equals(team.getAtivo())) {
            throw new IllegalArgumentException(
                    "Não é possível vincular o robô a uma equipe inativa."
            );
        }
    }

    private void validarInstituicaoAtiva(Institution institution) {
        if (!Boolean.TRUE.equals(institution.getAtivo())) {
            throw new IllegalArgumentException(
                    "Não é possível vincular o robô a uma instituição inativa."
            );
        }
    }

    private void validarNomeDuplicado(String nome, Long teamId) {
        if (robotRepository.existsByNomeIgnoreCaseAndTeamId(nome, teamId)) {
            throw new IllegalArgumentException(
                    "A equipe já possui um robô cadastrado com o nome: " + nome
            );
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(
            String nome,
            Long teamId,
            Long robotId) {

        if (robotRepository.existsByNomeIgnoreCaseAndTeamIdAndIdNot(
                nome,
                teamId,
                robotId
        )) {
            throw new IllegalArgumentException(
                    "A equipe já possui outro robô cadastrado com o nome: " + nome
            );
        }
    }

    private void normalizar(RobotDTO dto) {
        dto.setNome(dto.getNome().trim());

        if (dto.getDescricao() != null) {
            String descricao = dto.getDescricao().trim();
            dto.setDescricao(descricao.isBlank() ? null : descricao);
        }
    }

    private void preencherRobot(
            Robot robot,
            RobotDTO dto,
            Team team) {

        robot.setNome(dto.getNome());
        robot.setDescricao(dto.getDescricao());
        robot.setTeam(team);
    }
}