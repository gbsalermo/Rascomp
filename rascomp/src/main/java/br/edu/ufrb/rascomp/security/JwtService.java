package br.edu.ufrb.rascomp.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.edu.ufrb.rascomp.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.security.jwt.secret:}")
    private String secret;

    @Value("${app.security.jwt.expiration-ms:28800000}")
    private long expirationMs;

    public String gerarToken(UserAccount usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("role", usuario.getRole().name())
                .claim("userId", usuario.getId())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave())
                .compact();
    }

    public String extrairEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean tokenValido(String token, UserAccount usuario) {
        Claims claims = claims(token);
        return usuario.getEmail().equalsIgnoreCase(claims.getSubject())
                && claims.getExpiration().after(new Date())
                && usuario.isEnabled();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey chave() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina uma chave com pelo menos 32 caracteres no ambiente de execução.");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET deve possuir pelo menos 32 bytes.");
        }

        return Keys.hmacShaKeyFor(bytes);
    }
}
