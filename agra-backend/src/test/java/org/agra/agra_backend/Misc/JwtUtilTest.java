package org.agra.agra_backend.Misc;

import io.jsonwebtoken.Claims;
import org.agra.agra_backend.misc.JwtConfig;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Disabled in CI")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new StaticJwtConfig("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"));
    }

    @Test
    void generateTokenEmbedsUserInformation() {
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
        User user = new User();
        user.setId("user-456");
        user.setEmail("user2@example.com");
        user.setRole("STUDENT");

        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();

        User differentUser = new User();
        differentUser.setId("other-user");
        differentUser.setEmail("other@example.com");

        assertThat(jwtUtil.isTokenValid(token, differentUser)).isFalse();
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
