package br.edu.ufrb.rascomp.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.edu.ufrb.rascomp.dto.TeamDTO;
import br.edu.ufrb.rascomp.model.Team;
import br.edu.ufrb.rascomp.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamLogoService {

    private final TeamRepository teamRepository;
    private final TeamLogoStorageService storageService;

    @Transactional
    public TeamDTO atualizar(Long teamId, MultipartFile arquivo) {
        Team team = buscar(teamId);
        String storageKeyAnterior = team.getLogoStorageKey();

        TeamLogoStorageService.StoredLogo stored = storageService.armazenar(teamId, arquivo);
        team.setLogoStorageKey(stored.storageKey());
        team.setLogoOriginalFilename(stored.originalFilename());
        team.setLogoContentType(stored.contentType());

        Team salvo = teamRepository.save(team);

        if (storageKeyAnterior != null && !storageKeyAnterior.equals(stored.storageKey())) {
            storageService.remover(storageKeyAnterior);
        }

        return new TeamDTO(salvo);
    }

    @Transactional
    public TeamDTO remover(Long teamId) {
        Team team = buscar(teamId);
        String storageKeyAnterior = team.getLogoStorageKey();

        team.setLogoStorageKey(null);
        team.setLogoOriginalFilename(null);
        team.setLogoContentType(null);

        Team salvo = teamRepository.save(team);
        storageService.remover(storageKeyAnterior);
        return new TeamDTO(salvo);
    }

    @Transactional(readOnly = true)
    public TeamLogoFile carregarPublico(Long teamId) {
        Team team = buscar(teamId);

        if (team.getLogoStorageKey() == null || team.getLogoStorageKey().isBlank()) {
            throw new EntityNotFoundException("A equipe não possui logo pública.");
        }

        return new TeamLogoFile(
                storageService.carregar(team.getLogoStorageKey()),
                team.getLogoContentType(),
                team.getLogoOriginalFilename() == null ? "logo-equipe" : team.getLogoOriginalFilename());
    }

    private Team buscar(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada: " + teamId));
    }

    public record TeamLogoFile(Resource resource, String contentType, String filename) {}
}
