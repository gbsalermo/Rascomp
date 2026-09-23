package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.CompetitionDTO;
import br.edu.ufrb.rascomp.model.Competition;
import br.edu.ufrb.rascomp.model.Enum.StatusCompetition;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompetitionContextService {

    private final CompetitionRepository competitionRepository;

    @Transactional(readOnly = true)
    public List<CompetitionDTO> listarVisiveis(boolean apenasAtivas) {
        UserRole role = exigirOperador();

        if (role == UserRole.DEV) {
            return (apenasAtivas
                    ? competitionRepository.findByAtivoTrueOrderByDataInicioDesc()
                    : competitionRepository.findAllByOrderByDataInicioDesc())
                    .stream()
                    .map(CompetitionDTO::new)
                    .toList();
        }

        Competition vigente = buscarVigenteEntidade();
        return vigente == null ? List.of() : List.of(new CompetitionDTO(vigente));
    }

    @Transactional(readOnly = true)
    public CompetitionDTO buscarVigente() {
        exigirOperador();
        Competition vigente = buscarVigenteEntidade();
        return vigente == null ? null : new CompetitionDTO(vigente);
    }

    @Transactional(readOnly = true)
    public Competition exigirOperavel(Long competitionId) {
        UserRole role = exigirOperador();
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Competição não encontrada com o id: " + competitionId));

        if (role == UserRole.DEV) {
            return competition;
        }

        Competition vigente = buscarVigenteEntidade();
        if (vigente == null || !vigente.getId().equals(competitionId)) {
            throw new AccessDeniedException(
                    "A GESTÃO só pode operar a competição vigente.");
        }

        return competition;
    }

    @Transactional(readOnly = true)
    public CompetitionDTO buscarVisivel(Long competitionId) {
        return new CompetitionDTO(exigirOperavel(competitionId));
    }

    private UserRole exigirOperador() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        boolean dev = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DEV".equals(authority.getAuthority()));
        if (dev) return UserRole.DEV;

        boolean gestao = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_GESTAO".equals(authority.getAuthority()));
        if (gestao) return UserRole.GESTAO;

        throw new AccessDeniedException("Este perfil não pode operar competições.");
    }

    private Competition buscarVigenteEntidade() {
        List<Competition> ativas = competitionRepository.findByAtivoTrueOrderByDataInicioDesc();

        for (StatusCompetition status : List.of(
                StatusCompetition.EM_ANDAMENTO,
                StatusCompetition.INSCRICOES_ABERTAS,
                StatusCompetition.INSCRICOES_ENCERRADAS,
                StatusCompetition.PLANEJADA)) {
            Competition encontrada = ativas.stream()
                    .filter(item -> item.getStatus() == status)
                    .findFirst()
                    .orElse(null);
            if (encontrada != null) return encontrada;
        }

        return null;
    }
}
