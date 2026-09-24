package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.dto.UserAccountUpdateRequest;
import br.edu.ufrb.rascomp.model.Competitor;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.CompetitorRepository;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final CompetitorRepository competitorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserAccount cadastrarParticipante(RegisterRequest request) {
        return criar(request, UserRole.PARTICIPANTE);
    }

    @Transactional
    public UserAccountDTO criarDev(RegisterRequest request) {
        return criarInterno(request, UserRole.DEV);
    }

    @Transactional
    public UserAccountDTO criarInterno(RegisterRequest request, UserRole role) {
        if (role == null || role == UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException(
                    "Contas PARTICIPANTE devem ser criadas pelo cadastro comum.");
        }
        return new UserAccountDTO(criar(request, role));
    }

    @Transactional(readOnly = true)
    public UserAccount buscarAtual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserAccount usuario) {
            return usuario;
        }
        throw new EntityNotFoundException("Usuário autenticado não encontrado.");
    }

    @Transactional(readOnly = true)
    public UserAccount buscarPorId(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public UserAccount buscarPorEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<UserAccountDTO> listarPorRole(UserRole role) {
        return userAccountRepository.findByRoleOrderByNomeAsc(role)
                .stream()
                .map(this::toDTOComVinculoCompetidor)
                .toList();
    }

    @Transactional
    public UserAccount iniciarNovaSessao(UserAccount usuario) {
        usuario.setUltimoLogin(java.time.LocalDateTime.now());
        usuario.setSessionVersion(usuario.getSessionVersion() == null ? 1L : usuario.getSessionVersion() + 1L);
        return userAccountRepository.save(usuario);
    }

    @Transactional
    public void encerrarSessaoAtual() {
        UserAccount usuario = buscarAtual();
        usuario.setSessionVersion(usuario.getSessionVersion() == null ? 1L : usuario.getSessionVersion() + 1L);
        userAccountRepository.save(usuario);
    }

    @Transactional
    public UserAccountDTO atualizarDados(Long id, UserAccountUpdateRequest request) {
        UserAccount usuario = buscarPorId(id);
        String email = normalizarEmail(request.getEmail());

        if (userAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalArgumentException("Já existe uma conta cadastrada com este e-mail.");
        }

        Competitor competitor = usuario.getRole() == UserRole.PARTICIPANTE
                ? competitorRepository.findByUserAccountId(id).orElse(null)
                : null;

        if (competitor != null
                && competitorRepository.existsByEmailIgnoreCaseAndIdNot(email, competitor.getId())) {
            throw new IllegalArgumentException(
                    "Já existe outro competidor cadastrado com este e-mail.");
        }

        boolean alterouEmail = !usuario.getEmail().equalsIgnoreCase(email);
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(normalizarOpcional(request.getTelefone()));

        if (alterouEmail) {
            usuario.setSessionVersion(usuario.getSessionVersion() == null ? 1L : usuario.getSessionVersion() + 1L);
        }

        UserAccount salvo = userAccountRepository.save(usuario);

        if (competitor != null) {
            competitor.setNome(salvo.getNome());
            competitor.setEmail(salvo.getEmail());
            competitor.setTelefone(salvo.getTelefone());
            competitorRepository.save(competitor);
        }

        return toDTOComVinculoCompetidor(salvo);
    }

    @Transactional
    public UserAccountDTO alterarAtivo(Long id, boolean ativo) {
        UserAccount usuario = buscarPorId(id);

        if (!ativo && ehUsuarioAtual(usuario)) {
            throw new IllegalArgumentException("A conta atualmente autenticada não pode ser desativada.");
        }

        if (!ativo && usuario.getRole() == UserRole.DEV && Boolean.TRUE.equals(usuario.getAtivo())) {
            validarNaoEhUltimoDevAtivo();
        }

        usuario.setAtivo(ativo);
        if (!ativo) {
            usuario.setSessionVersion(usuario.getSessionVersion() == null ? 1L : usuario.getSessionVersion() + 1L);
        }

        UserAccount salvo = userAccountRepository.save(usuario);

        if (salvo.getRole() == UserRole.PARTICIPANTE) {
            competitorRepository.findByUserAccountId(salvo.getId()).ifPresent(competitor -> {
                competitor.setAtivo(ativo);
                competitorRepository.save(competitor);
            });
        }

        return toDTOComVinculoCompetidor(salvo);
    }

    @Transactional
    public UserAccountDTO alterarRoleInterna(Long id, UserRole novaRole) {
        if (novaRole == null || novaRole == UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException(
                    "Contas internas só podem usar as roles DEV, GESTAO ou MIDIA.");
        }

        UserAccount usuario = buscarPorId(id);
        if (usuario.getRole() == UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException(
                    "Conta PARTICIPANTE é uma identidade separada e não pode ser convertida em conta interna.");
        }

        if (ehUsuarioAtual(usuario)) {
            throw new IllegalArgumentException(
                    "A conta atualmente autenticada não pode alterar a própria permissão.");
        }

        if (usuario.getRole() == novaRole) {
            return new UserAccountDTO(usuario);
        }

        if (usuario.getRole() == UserRole.DEV
                && novaRole != UserRole.DEV
                && Boolean.TRUE.equals(usuario.getAtivo())) {
            validarNaoEhUltimoDevAtivo();
        }

        usuario.setRole(novaRole);
        return new UserAccountDTO(userAccountRepository.save(usuario));
    }

    private UserAccountDTO toDTOComVinculoCompetidor(UserAccount usuario) {
        UserAccountDTO dto = new UserAccountDTO(usuario);

        if (usuario.getRole() != UserRole.PARTICIPANTE || usuario.getId() == null) {
            return dto;
        }

        competitorRepository.findByUserAccountId(usuario.getId()).ifPresent(competitor -> {
            dto.setCompetitorId(competitor.getId());
            dto.setCompetitorNome(competitor.getNome());
            dto.setCompetitorAtivo(competitor.getAtivo());

            if (competitor.getTeam() != null) {
                dto.setCompetitorTeamId(competitor.getTeam().getId());
                dto.setCompetitorTeamNome(competitor.getTeam().getNome());
                dto.setTeamWithoutActiveCompetitors(
                        competitorRepository.countByTeamIdAndAtivoTrue(competitor.getTeam().getId()) == 0);
            }
        });

        return dto;
    }

    private void validarNaoEhUltimoDevAtivo() {
        if (userAccountRepository.countByRoleAndAtivoTrue(UserRole.DEV) <= 1) {
            throw new IllegalArgumentException(
                    "O RasComp deve manter pelo menos um DEV ativo.");
        }
    }

    private boolean ehUsuarioAtual(UserAccount usuario) {
        String emailAutenticado = emailAutenticado();
        return emailAutenticado != null && usuario.getEmail().equalsIgnoreCase(emailAutenticado);
    }

    private String emailAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authentication.getName();
    }

    private UserAccount criar(RegisterRequest request, UserRole role) {
        String email = normalizarEmail(request.getEmail());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe uma conta cadastrada com este e-mail.");
        }

        UserAccount usuario = new UserAccount();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.getSenha()));
        usuario.setTelefone(normalizarOpcional(request.getTelefone()));
        usuario.setRole(role);
        usuario.setAtivo(true);
        return userAccountRepository.save(usuario);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
