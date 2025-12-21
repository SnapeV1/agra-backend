package org.agra.agra_backend.misc;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

    @Test
    void generateTokenEmbedsUserInformation() {
        JwtUtil jwtUtil = new JwtUtil(new StaticJwtConfig(SECRET));

        User user = new User();
        user.setId("user-123");
        user.setEmail("user@example.com");
        user.setRole("ADMIN");

        String token = jwtUtil.generateToken(user);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValidMatchesUserAndExpiration() {
        JwtUtil jwtUtil = new JwtUtil(new StaticJwtConfig(SECRET));

        User user = new User();
        user.setId("user-456");

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();

        User differentUser = new User();
        differentUser.setId("other-user");

        assertThat(jwtUtil.isTokenValid(token, differentUser)).isFalse();
    }

    @Test
    void extractUserIdReturnsSubject() {
        JwtUtil jwtUtil = new JwtUtil(new StaticJwtConfig(SECRET));

        User user = new User();
        user.setId("user-999");

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-999");
    }

    @Test
    void isTokenExpiredDetectsExpiredTokens() {
        JwtUtil jwtUtil = new JwtUtil(new StaticJwtConfig(SECRET));
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .setSubject("user-1")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtUtil.isTokenExpired(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenExpiredReturnsFalseForValidToken() {
        JwtUtil jwtUtil = new JwtUtil(new StaticJwtConfig(SECRET));
        User user = new User();
        user.setId("user-1");

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    private static final class StaticJwtConfig extends JwtConfig {
        private final String fixedSecret;

        private StaticJwtConfig(String fixedSecret) {
            this.fixedSecret = fixedSecret;
        }

        @Override
        public String getSecret() {
            return fixedSecret;
        }
    }
}
