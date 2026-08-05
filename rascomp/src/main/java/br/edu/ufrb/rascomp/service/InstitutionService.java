package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.InstitutionDTO;
import br.edu.ufrb.rascomp.model.Institution;
import br.edu.ufrb.rascomp.repository.InstitutionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionService {
	
	private final InstitutionRepository institutionRepository;
	
	@Transactional
	public InstitutionDTO criar(InstitutionDTO dto) {
		
		normalizar(dto);
		validarSiglaDuplicada(dto.getSigla());
		
		Institution institution = new Institution();
		preencherInstitution(institution, dto);
		institution.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
		
		Institution salva = institutionRepository.save(institution);
		
		return new InstitutionDTO(salva);
	}

	@Transactional(readOnly = true)
	public List<InstitutionDTO> listarTodos(){
		
		return institutionRepository
				.findAllByOrderByNomeAsc()
				.stream()
				.map(InstitutionDTO::new)
				.toList();
		
	}
	
	@Transactional(readOnly = true)
	public List<InstitutionDTO> listarAtivas(){
		
		return institutionRepository
				.findByAtivoTrueOrderByNomeAsc()
				.stream()
				.map(InstitutionDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public InstitutionDTO buscarPorId(Long id) {
		return new InstitutionDTO(buscarEntidade(id));
	}
	
	@Transactional(readOnly = true)
	public InstitutionDTO buscarPorSigla(String sigla) {
		Institution institution = institutionRepository
				.findBySiglaIgnoreCase(sigla.trim())
				.orElseThrow(() -> new EntityNotFoundException(
						"Instituição não encontrada com a sigla: " + sigla));
		return new InstitutionDTO(institution);
	}
	
	@Transactional
	public InstitutionDTO atualizar(Long id, InstitutionDTO dto) {
		Institution institution = buscarEntidade(id);
		
		normalizar(dto);
		validarSiglaDuplicadaNaAtualizacao(dto.getSigla(), id);
		preencherInstitution(institution, dto);
		
		if(dto.getAtivo() != null) {
			institution.setAtivo(dto.getAtivo());
		}
		
		Institution atualizada = institutionRepository.save(institution);
		
		return new InstitutionDTO(atualizada);
	}
	
	@Transactional
	public void deletar(Long id) {
		Institution institution = buscarEntidade(id);
		institution.setAtivo(false);
		institutionRepository.save(institution);
	}
	
	@Transactional
	public InstitutionDTO reativar(Long id) {
		Institution institution = buscarEntidade(id);
		institution.setAtivo(true);
		
		Institution reativada = institutionRepository.save(institution);
		
		return new InstitutionDTO(reativada);
	}
	
	private Institution buscarEntidade(Long id) {
		return institutionRepository
				.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(
						"Instituição não encontrada com o id: " + id ));
	}
	
	private void validarSiglaDuplicada(String sigla) {
		if(institutionRepository.existsBySiglaIgnoreCase(sigla)) {
			throw new IllegalArgumentException(
					"Já existe uma instituição cadastrada com a sigla: " + sigla);		
		}}
		
	private void validarSiglaDuplicadaNaAtualizacao(String sigla, Long id) {
		
		if(institutionRepository.existsBySiglaIgnoreCaseAndIdNot(sigla, id)) {
			
			throw new IllegalArgumentException(
					"Já existe outra instituição cadastrada com a sigla: " + sigla);
		}
	}
	
	private void normalizar(InstitutionDTO dto) {
        dto.setNome(dto.getNome().trim());
        dto.setSigla(dto.getSigla().trim().toUpperCase());

        if (dto.getCidade() != null) {
            dto.setCidade(dto.getCidade().trim());
        }

        if (dto.getEstado() != null) {
            dto.setEstado(dto.getEstado().trim().toUpperCase());
        }
    }

    private void preencherInstitution(
            Institution institution,
            InstitutionDTO dto) {

        institution.setNome(dto.getNome());
        institution.setSigla(dto.getSigla());
        institution.setCidade(dto.getCidade());
        institution.setEstado(dto.getEstado());
    }
}
