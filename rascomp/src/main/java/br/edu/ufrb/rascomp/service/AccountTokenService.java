package br.edu.ufrb.rascomp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ufrb.rascomp.model.AccountToken;
import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.AccountTokenType;
import br.edu.ufrb.rascomp.repository.AccountTokenRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountTokenService {

    private final AccountTokenRepository accountTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public record IssuedToken(String rawToken, LocalDateTime expiresAt) {
    }

    @Transactional
    public IssuedToken issue(UserAccount userAccount, AccountTokenType type, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();

        var ativos = accountTokenRepository.findByUserAccountIdAndTypeAndUsedAtIsNull(
                userAccount.getId(),
                type);

        ativos.forEach(token -> token.setUsedAt(now));
        if (!ativos.isEmpty()) {
            accountTokenRepository.saveAll(ativos);
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = now.plus(ttl);

        AccountToken token = new AccountToken();
        token.setUserAccount(userAccount);
        token.setType(type);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(now);
        accountTokenRepository.save(token);

        return new IssuedToken(raw, expiresAt);
    }

    @Transactional
    public UserAccount consume(String rawToken, AccountTokenType type) {
        LocalDateTime now = LocalDateTime.now();

        AccountToken token = accountTokenRepository
                .findByTokenHashAndTypeAndUsedAtIsNull(hash(rawToken), type)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou já utilizado."));

        if (!token.getExpiresAt().isAfter(now)) {
            token.setUsedAt(now);
            accountTokenRepository.save(token);
            throw new IllegalArgumentException("Token expirado. Solicite um novo link.");
        }

        token.setUsedAt(now);
        accountTokenRepository.save(token);
        return token.getUserAccount();
    }

    @Transactional(readOnly = true)
    public boolean isInCooldown(
            UserAccount userAccount,
            AccountTokenType type,
            Duration cooldown) {
        LocalDateTime limit = LocalDateTime.now().minus(cooldown);
        return accountTokenRepository
                .findFirstByUserAccountIdAndTypeOrderByCreatedAtDesc(userAccount.getId(), type)
                .map(AccountToken::getCreatedAt)
                .map(createdAt -> createdAt.isAfter(limit))
                .orElse(false);
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }
}
