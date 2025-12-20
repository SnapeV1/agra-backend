package org.agra.agra_backend.misc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JitsiTokenService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JitsiTokenService.class);
    private static final String HTTPS_SCHEME = "https://";

    @Value("${app-id}")
    private String appId;

    @Value("${app-secret}")
    private String appSecret;

    @Value("${sub}")
    private String sub;

    @Value("${jitsi.room-claim:*}")
    private String roomClaim;

    @Value("${jitsi.token-ttl-seconds:3600}")
    private long tokenTtlSeconds;

    public String mintUserToken(User user, Session s, boolean moderator) {
        log.info("[JITSI][Token] mintUserToken start");
        log.info("[JITSI][Token] userId={}, name={}, email={}, moderator={}", user.getId(), user.getName(), user.getEmail(), moderator);
        log.info("[JITSI][Token] sessionId={}, room={}, courseId={}", s.getId(), s.getRoomName(), s.getCourseId());
        String normalizedSub = normalizeHost(sub);
        log.info("[JITSI][Token] Using appId={}, sub(raw)={}, sub(normalized)={}, ttlSeconds={}, secretLength={}",
                appId, sub, normalizedSub, tokenTtlSeconds, appSecret == null ? 0 : appSecret.length());
        String expectedBaseUrl = normalizedSub == null || normalizedSub.isEmpty() ? "N/A" : HTTPS_SCHEME + normalizedSub + ":8443";
        log.info("[JITSI][Token] Expected connect URL (https:8443)={}", expectedBaseUrl);
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("Jitsi app secret is not configured.");
        }
        Instant now = Instant.now();

        Map<String, Object> userCtx = new HashMap<>();
        userCtx.put("name", user.getName());
        userCtx.put("id", user.getId());
        if (user.getEmail() != null) userCtx.put("email", user.getEmail());
        if (user.getPicture() != null) userCtx.put("avatar", user.getPicture());

        Map<String, Object> context = new HashMap<>();
        context.put("user", userCtx);
        context.put("moderator", moderator);

        String room = ("*".equals(roomClaim) || roomClaim == null || roomClaim.isBlank()) ? "*" : s.getRoomName();
        log.info("[JITSI][Token] roomClaimConfig={}, effectiveRoomClaim={}", roomClaim, room);

        String token = Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setAudience("jitsi")
                .setIssuer(appId)
                .setSubject(normalizedSub)
                .claim("room", room)
                .claim("context", context)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(Duration.ofSeconds(tokenTtlSeconds))))
                .signWith(
                        Keys.hmacShaKeyFor(appSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
        log.info("[JITSI][Token] Token generated successfully. length={}", token.length());
        return token;
    }

    /**
     * Normalizes a host/tenant to the expected bare host: strips scheme, port, and trailing slashes.
     */
    private static String normalizeHost(String raw) {
        if (raw == null) return null;
        String val = raw.trim();
        if (val.startsWith("http://")) val = val.substring("http://".length());
        else if (val.startsWith(HTTPS_SCHEME)) val = val.substring(HTTPS_SCHEME.length());
        if (val.contains("/")) val = val.substring(0, val.indexOf("/"));
        // Remove port if present
        if (val.contains(":")) val = val.substring(0, val.indexOf(":"));
        return val;
    }
}
