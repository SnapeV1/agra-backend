package org.agra.agra_backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.agra.agra_backend.config.JitsiConfig;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class JitsiTokenService {

    private final JitsiConfig config;
    private final SecretKey secretKey;

    public JitsiTokenService(JitsiConfig config) {
        this.config = config;
        byte[] secretBytes = config.getAppSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("Jitsi app-secret must be >= 32 bytes for HS256 (RFC 7518 §3.2). Current length: " + secretBytes.length + " bytes.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Generate a Jitsi JWT for the given room and user.
     * Includes moderator claim and context.user fields as needed by Jitsi.
     */
    public String generateToken(String room, String username, boolean isCreator) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(config.getTokenTtlSeconds());

        Map<String, Object> contextUser = new HashMap<>();
        if (username != null && !username.isBlank()) {
            contextUser.put("name", username);
        }
        contextUser.put("moderator", isCreator);

        Map<String, Object> context = new HashMap<>();
        context.put("user", contextUser);

        return Jwts.builder()
                .setIssuer(config.getAppId())
                .setAudience(config.getAudience())
                .setSubject(config.getDomain())
                .claim("room", room)
                .claim("moderator", isCreator)
                .claim("context", context)
                .setIssuedAt(java.util.Date.from(now))
                .setNotBefore(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(exp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}