package br.edu.ufrb.rascomp.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.dto.RegisterRequest;
import br.edu.ufrb.rascomp.dto.UserAccountDTO;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
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
                .map(UserAccountDTO::new)
                .toList();
    }

    @Transactional
    public void registrarLogin(UserAccount usuario) {
        usuario.setUltimoLogin(java.time.LocalDateTime.now());
        userAccountRepository.save(usuario);
    }

    @Transactional
    public UserAccountDTO alterarAtivo(Long id, boolean ativo) {
        UserAccount usuario = buscarPorId(id);
        if (!ativo && usuario.getRole() == UserRole.DEV && Boolean.TRUE.equals(usuario.getAtivo())) {
            validarNaoEhUltimoDevAtivo();
        }
        usuario.setAtivo(ativo);
        return new UserAccountDTO(userAccountRepository.save(usuario));
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

        String emailAutenticado = emailAutenticado();
        if (emailAutenticado != null && usuario.getEmail().equalsIgnoreCase(emailAutenticado)) {
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

    private void validarNaoEhUltimoDevAtivo() {
        if (userAccountRepository.countByRoleAndAtivoTrue(UserRole.DEV) <= 1) {
            throw new IllegalArgumentException(
                    "O RasComp deve manter pelo menos um DEV ativo.");
        }
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
