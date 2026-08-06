package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {
	
	
	private final TeamRepository teamRepository;
	private final InstitutionRepository institutionRepository;
	
	
	@Transactional
	public TeamDTO criar(TeamDTO dto) {
		normalizar(dto);
		
		Institution institution = buscarInstituicao(dto.getInstitutionId());
		
		validarInstituicaoAtiva(institution);
		
		validarNomeDuplicado(dto.getNome(), institution.getId());
		
		Team team = new Team();
		preencherTeam(team, dto, institution);
		
		team.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
		
		Team salva = teamRepository.save(team);
		
		return new TeamDTO(salva);
	}
	
	@Transactional(readOnly = true)
	public List<TeamDTO> listarTodos(){
		return teamRepository
				.findAllByOrderByNomeAsc()
				.stream()
				.map(TeamDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<TeamDTO> listarAtivas(){
		return teamRepository
				.findByAtivoTrueOrderByNomeAsc()
				.stream()
				.map(TeamDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public TeamDTO buscarPorId(Long id) {
		return new TeamDTO(buscarEntidade(id));
	}
	
	@Transactional(readOnly = true)
	public List<TeamDTO> listarPorInstituicao( Long institutionId, boolean apenasAtivas){
		
		buscarInstituicao(institutionId);
		
		List<Team> teams;
		
		if(apenasAtivas) {
			teams = teamRepository.findByInstitutionIdAndAtivoTrueOrderByNomeAsc(institutionId);
			
		} else {
			teams = teamRepository.findByInstitutionIdOrderByNomeAsc(institutionId);
		}
		
		return teams
				.stream()
				.map(TeamDTO::new)
				.toList();
		
	}
	
	
	@Transactional
	public TeamDTO atualizar(Long id, TeamDTO dto) {
		Team team = buscarEntidade(id);
		
		normalizar(dto);
		
		Institution institution = buscarInstituicao(dto.getInstitutionId());
		validarInstituicaoAtiva(institution);
		validarNomeDuplicadoNaAtualizacao(dto.getNome(), institution.getId(), id);
	
		preencherTeam(team, dto, institution);
		
		if(dto.getAtivo() != null) {
			team.setAtivo(dto.getAtivo());
		}
		
		Team atualizada = teamRepository.save(team);
		
		return new TeamDTO(atualizada);
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
		
		Team reativada = teamRepository.save(team);
		
		return new TeamDTO(reativada);
	}
	
	private Team buscarEntidade(Long id) {
		return teamRepository
				.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada com o id: " + id));
	}
	
	private Institution buscarInstituicao(Long institutionId) {
        return institutionRepository
                .findById(institutionId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Instituição não encontrada com o id: "
                                        + institutionId
                        )
                );
	}
	
	private void validarInstituicaoAtiva(
            Institution institution) {

        if (!Boolean.TRUE.equals(institution.getAtivo())) {
            throw new IllegalArgumentException(
                    "Não é possível vincular a equipe "
                    + "a uma instituição inativa."
            );
        }
    }

    private void validarNomeDuplicado(
            String nome,
            Long institutionId) {

        boolean existe = teamRepository
                .existsByNomeIgnoreCaseAndInstitutionId(
                        nome,
                        institutionId
                );

        if (existe) {
            throw new IllegalArgumentException(
                    "A instituição já possui uma equipe "
                    + "cadastrada com o nome: " + nome
            );
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(
            String nome,
            Long institutionId,
            Long teamId) {

        boolean existe = teamRepository
                .existsByNomeIgnoreCaseAndInstitutionIdAndIdNot(
                        nome,
                        institutionId,
                        teamId
                );

        if (existe) {
            throw new IllegalArgumentException(
                    "A instituição já possui outra equipe "
                    + "cadastrada com o nome: " + nome
            );
        }
    }

    private void normalizar(TeamDTO dto) {
        dto.setNome(dto.getNome().trim());
    }

    private void preencherTeam(
            Team team,
            TeamDTO dto,
            Institution institution) {

        team.setNome(dto.getNome());
        team.setInstitution(institution);
    }
}

