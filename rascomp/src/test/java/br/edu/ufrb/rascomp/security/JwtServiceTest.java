package br.edu.ufrb.rascomp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.edu.ufrb.rascomp.model.UserAccount;
import br.edu.ufrb.rascomp.model.Enum.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET =
            "rascomp-etapa-4-test-secret-key-with-more-than-thirty-two-bytes";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtService, "rememberExpirationMs", 600_000L);
    }

    @Test
    void lembrarDeMimDeveGerarTokenComValidadeMaior() {
        UserAccount usuario = usuario();

        Claims sessao = claims(jwtService.gerarToken(usuario, false));
        Claims lembrado = claims(jwtService.gerarToken(usuario, true));

        long validadeSessao = duracao(sessao);
        long validadeLembrado = duracao(lembrado);

        assertTrue(validadeSessao >= 59_000L);
        assertTrue(validadeLembrado >= 599_000L);
        assertTrue(validadeLembrado > validadeSessao);
    }

    @Test
    void tokenAnteriorDeveSerInvalidoQuandoVersaoDaSessaoMudar() {
        UserAccount usuario = usuario();
        usuario.setSessionVersion(3L);
        String token = jwtService.gerarToken(usuario, false);

        assertTrue(jwtService.tokenValido(token, usuario));

        usuario.setSessionVersion(4L);

        assertFalse(jwtService.tokenValido(token, usuario));
    }

    @Test
    void tokenDeveSerInvalidoQuandoUsuarioForDesativado() {
        UserAccount usuario = usuario();
        String token = jwtService.gerarToken(usuario, false);

        assertTrue(jwtService.tokenValido(token, usuario));

        usuario.setAtivo(false);

        assertFalse(jwtService.tokenValido(token, usuario));
    }

    private long duracao(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        return expiration.getTime() - issuedAt.getTime();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private UserAccount usuario() {
        UserAccount usuario = new UserAccount();
        usuario.setId(1L);
        usuario.setNome("Participante");
        usuario.setEmail("usuario@exemplo.com");
        usuario.setRole(UserRole.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setSessionVersion(1L);
        return usuario;
    }
}
