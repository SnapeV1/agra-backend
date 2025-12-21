package org.agra.agra_backend.misc;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JitsiTokenServiceTest {

    @Test
    void mintUserTokenRequiresSecret() {
        JitsiTokenService service = new JitsiTokenService();
        ReflectionTestUtils.setField(service, "appSecret", " ");

        assertThatThrownBy(() -> service.mintUserToken(new User(), new Session(), false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mintUserTokenBuildsJwt() {
        JitsiTokenService service = new JitsiTokenService();
        String secret = "0123456789abcdef0123456789abcdef";
        ReflectionTestUtils.setField(service, "appId", "app-1");
        ReflectionTestUtils.setField(service, "appSecret", secret);
        ReflectionTestUtils.setField(service, "sub", "https://meet.example.com:8443/tenant");
        ReflectionTestUtils.setField(service, "roomClaim", "*");
        ReflectionTestUtils.setField(service, "tokenTtlSeconds", 60L);

        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        user.setEmail("alice@example.com");
        Session session = new Session();
        session.setId("session-1");
        session.setCourseId("course-1");
        session.setRoomName("room-1");
        session.setStartTime(Instant.now());
        session.setEndTime(Instant.now().plusSeconds(3600));

        String token = service.mintUserToken(user, session, true);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getIssuer()).isEqualTo("app-1");
        assertThat(claims.getSubject()).isEqualTo("meet.example.com");
        assertThat(claims.get("room")).isEqualTo("*");
    }

    @Test
    void mintUserTokenUsesRoomNameWhenRoomClaimFixed() {
        JitsiTokenService service = new JitsiTokenService();
        String secret = "0123456789abcdef0123456789abcdef";
        ReflectionTestUtils.setField(service, "appId", "app-2");
        ReflectionTestUtils.setField(service, "appSecret", secret);
        ReflectionTestUtils.setField(service, "sub", "https://meet.example.com:8443/tenant/path");
        ReflectionTestUtils.setField(service, "roomClaim", "single-room");
        ReflectionTestUtils.setField(service, "tokenTtlSeconds", 120L);

        User user = new User();
        user.setId("user-2");
        user.setName("Bob");
        user.setPicture("https://img.example.com/bob.png");
        Session session = new Session();
        session.setId("session-2");
        session.setCourseId("course-2");
        session.setRoomName("room-2");

        String token = service.mintUserToken(user, session, false);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("meet.example.com");
        assertThat(claims.get("room")).isEqualTo("room-2");
    }

    @Test
    void normalizeHostHandlesNullAndHttp() {
        String normalizedNull = ReflectionTestUtils.invokeMethod(JitsiTokenService.class, "normalizeHost", (Object) null);
        String normalizedHttp = ReflectionTestUtils.invokeMethod(JitsiTokenService.class, "normalizeHost", "http://example.com/tenant");

        assertThat(normalizedNull).isNull();
        assertThat(normalizedHttp).isEqualTo("example.com");
    }
}
