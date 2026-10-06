package br.edu.ufrb.rascomp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.AccountTokenType;
import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountIdentityService {

    private static final String GENERIC_VERIFICATION_MESSAGE =
            "Se a conta existir e ainda precisar de confirmação, enviaremos um novo link.";
    private static final String GENERIC_RECOVERY_MESSAGE =
            "Se existir uma conta elegível para esse e-mail, enviaremos as instruções de recuperação.";

    private final UserAccountRepository userAccountRepository;
    private final AccountTokenService accountTokenService;
    private final TransactionalEmailService transactionalEmailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.identity.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.identity.email-verification-hours:24}")
    private long verificationHours;

    @Value("${app.identity.password-reset-minutes:30}")
    private long resetMinutes;

    @Value("${app.identity.internal-account-setup-hours:24}")
    private long internalSetupHours;

    @Value("${app.identity.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    public void sendInitialVerification(UserAccount userAccount) {
        sendVerification(userAccount);
    }

    public String resendVerification(String email) {
        Optional<UserAccount> optional = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email));
        if (optional.isEmpty()) {
            return GENERIC_VERIFICATION_MESSAGE;
        }

        UserAccount userAccount = optional.get();
        if (!Boolean.TRUE.equals(userAccount.getAtivo()) || userAccount.isEmailVerified()) {
            return GENERIC_VERIFICATION_MESSAGE;
        }

        if (userAccount.getRole() != br.edu.ufrb.rascomp.model.Enum.UserRole.PARTICIPANTE) {
            if (!accountTokenService.isInCooldown(
                    userAccount,
                    AccountTokenType.INTERNAL_ACCOUNT_SETUP,
                    Duration.ofSeconds(cooldownSeconds))) {
                sendInternalAccountSetup(userAccount);
            }
            return GENERIC_VERIFICATION_MESSAGE;
        }

        if (accountTokenService.isInCooldown(
                userAccount,
                AccountTokenType.EMAIL_VERIFICATION,
                Duration.ofSeconds(cooldownSeconds))) {
            return GENERIC_VERIFICATION_MESSAGE;
        }

        sendVerification(userAccount);
        return GENERIC_VERIFICATION_MESSAGE;
    }

    @Transactional
    public String confirmEmail(String rawToken) {
        UserAccount userAccount =
                accountTokenService.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION);

        if (!userAccount.isEmailVerified()) {
            userAccount.setEmailVerificadoEm(LocalDateTime.now());
            userAccount.setSessionVersion(nextSessionVersion(userAccount));
            userAccountRepository.save(userAccount);
        }

        return "E-mail confirmado com sucesso. Você já pode entrar no RasComp.";
    }

    public String requestPasswordReset(String email) {
        Optional<UserAccount> optional = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email));
        if (optional.isEmpty()) {
            return GENERIC_RECOVERY_MESSAGE;
        }

        UserAccount userAccount = optional.get();
        if (!Boolean.TRUE.equals(userAccount.getAtivo()) || !userAccount.isEmailVerified()) {
            return GENERIC_RECOVERY_MESSAGE;
        }

        if (accountTokenService.isInCooldown(
                userAccount,
                AccountTokenType.PASSWORD_RESET,
                Duration.ofSeconds(cooldownSeconds))) {
            return GENERIC_RECOVERY_MESSAGE;
        }

        var issued = accountTokenService.issue(
                userAccount,
                AccountTokenType.PASSWORD_RESET,
                Duration.ofMinutes(resetMinutes));

        String link = normalizedBaseUrl() + "/redefinir-senha?token=" + issued.rawToken();
        transactionalEmailService.send(
                userAccount.getEmail(),
                "RasComp — redefinição de senha",
                recoveryEmailHtml(userAccount.getNome(), link));

        return GENERIC_RECOVERY_MESSAGE;
    }

    public void sendInternalAccountSetup(UserAccount userAccount) {
        var issued = accountTokenService.issue(
                userAccount,
                AccountTokenType.INTERNAL_ACCOUNT_SETUP,
                Duration.ofHours(internalSetupHours));

        String link = normalizedBaseUrl() + "/ativar-conta?token=" + issued.rawToken();
        transactionalEmailService.send(
                userAccount.getEmail(),
                "RasComp — ative sua conta",
                internalAccountSetupEmailHtml(userAccount.getNome(), link));
    }

    public String resendInternalAccountSetup(UserAccount userAccount) {
        if (userAccount.getRole() == br.edu.ufrb.rascomp.model.Enum.UserRole.PARTICIPANTE) {
            throw new IllegalArgumentException("Conta PARTICIPANTE não usa convite interno.");
        }
        if (!Boolean.TRUE.equals(userAccount.getAtivo())) {
            throw new IllegalArgumentException("A conta interna está inativa.");
        }
        if (userAccount.isEmailVerified()) {
            throw new IllegalArgumentException("A conta interna já foi ativada.");
        }
        if (accountTokenService.isInCooldown(
                userAccount,
                AccountTokenType.INTERNAL_ACCOUNT_SETUP,
                Duration.ofSeconds(cooldownSeconds))) {
            return "Um convite foi enviado recentemente. Aguarde antes de solicitar outro.";
        }

        sendInternalAccountSetup(userAccount);
        return "Convite de primeiro acesso enviado.";
    }

    @Transactional
    public String activateInternalAccount(String rawToken, String newPassword) {
        UserAccount userAccount =
                accountTokenService.consume(rawToken, AccountTokenType.INTERNAL_ACCOUNT_SETUP);

        if (userAccount.getRole() == br.edu.ufrb.rascomp.model.Enum.UserRole.PARTICIPANTE
                || !Boolean.TRUE.equals(userAccount.getAtivo())
                || userAccount.isEmailVerified()) {
            throw new IllegalArgumentException("Este convite de primeiro acesso não pode mais ser utilizado.");
        }

        userAccount.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccount.setEmailVerificadoEm(LocalDateTime.now());
        userAccount.setSessionVersion(nextSessionVersion(userAccount));
        userAccountRepository.save(userAccount);

        return "Conta ativada com sucesso. Entre no RasComp com a senha que você definiu.";
    }

    @Transactional
    public String resetPassword(String rawToken, String newPassword) {
        UserAccount userAccount =
                accountTokenService.consume(rawToken, AccountTokenType.PASSWORD_RESET);

        if (!Boolean.TRUE.equals(userAccount.getAtivo()) || !userAccount.isEmailVerified()) {
            throw new IllegalArgumentException("Este link de recuperação não pode mais ser utilizado.");
        }

        userAccount.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccount.setSessionVersion(nextSessionVersion(userAccount));
        userAccountRepository.save(userAccount);

        return "Senha alterada com sucesso. Entre novamente com a nova senha.";
    }

    private void sendVerification(UserAccount userAccount) {
        var issued = accountTokenService.issue(
                userAccount,
                AccountTokenType.EMAIL_VERIFICATION,
                Duration.ofHours(verificationHours));

        String link = normalizedBaseUrl() + "/verificar-email?token=" + issued.rawToken();
        transactionalEmailService.send(
                userAccount.getEmail(),
                "RasComp — confirme seu e-mail",
                verificationEmailHtml(userAccount.getNome(), link));
    }

    private String verificationEmailHtml(String name, String link) {
        return "<p>Olá, " + safeName(name) + ".</p>"
                + "<p>Confirme o e-mail da sua conta RasComp pelo link abaixo:</p>"
                + "<p><a href=\"" + link + "\">Confirmar e-mail</a></p>"
                + "<p>O link é de uso único e expira em " + verificationHours + " horas.</p>";
    }

    private String internalAccountSetupEmailHtml(String name, String link) {
        return "<p>Olá, " + safeName(name) + ".</p>"
                + "<p>Uma conta interna do RasComp foi criada para o seu e-mail.</p>"
                + "<p>O administrador não definiu sua senha. Use o link abaixo para ativar a conta e criar sua própria senha:</p>"
                + "<p><a href=\"" + link + "\">Ativar conta e definir senha</a></p>"
                + "<p>O link é de uso único e expira em " + internalSetupHours + " horas.</p>";
    }

    private String recoveryEmailHtml(String name, String link) {
        return "<p>Olá, " + safeName(name) + ".</p>"
                + "<p>Recebemos um pedido para redefinir sua senha do RasComp.</p>"
                + "<p><a href=\"" + link + "\">Redefinir senha</a></p>"
                + "<p>O link é de uso único e expira em " + resetMinutes + " minutos.</p>"
                + "<p>Se você não solicitou a alteração, ignore esta mensagem.</p>";
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "participante";
        }
        return name.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String normalizedBaseUrl() {
        if (frontendBaseUrl.endsWith("/")) {
            return frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1);
        }
        return frontendBaseUrl;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private long nextSessionVersion(UserAccount userAccount) {
        return userAccount.getSessionVersion() == null
                ? 1L
                : userAccount.getSessionVersion() + 1L;
    }
}
