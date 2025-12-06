package org.agra.agra_backend.Misc;

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
        System.out.println("[JITSI][Token] mintUserToken start");
        System.out.println("[JITSI][Token] userId=" + user.getId() + ", name=" + user.getName() + ", email=" + user.getEmail() + ", moderator=" + moderator);
        System.out.println("[JITSI][Token] sessionId=" + s.getId() + ", room=" + s.getRoomName() + ", courseId=" + s.getCourseId());
        String normalizedSub = normalizeHost(sub);
        System.out.println("[JITSI][Token] Using appId=" + appId + ", sub(raw)=" + sub + ", sub(normalized)=" + normalizedSub + ", ttlSeconds=" + tokenTtlSeconds + ", secretLength=" + (appSecret == null ? 0 : appSecret.length()));
        String expectedBaseUrl = normalizedSub == null || normalizedSub.isEmpty() ? "N/A" : "https://" + normalizedSub + ":8443";
        System.out.println("[JITSI][Token] Expected connect URL (https:8443)=" + expectedBaseUrl);
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
        System.out.println("[JITSI][Token] roomClaimConfig=" + roomClaim + ", effectiveRoomClaim=" + room);

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
        System.out.println("[JITSI][Token] Token generated successfully. length=" + token.length());
        return token;
    }

    /**
     * Normalizes a host/tenant to the expected bare host: strips scheme, port, and trailing slashes.
     */
    private static String normalizeHost(String raw) {
        if (raw == null) return null;
        String val = raw.trim();
        if (val.startsWith("http://")) val = val.substring("http://".length());
        else if (val.startsWith("https://")) val = val.substring("https://".length());
        if (val.contains("/")) val = val.substring(0, val.indexOf("/"));
        // Remove port if present
        if (val.contains(":")) val = val.substring(0, val.indexOf(":"));
        return val;
    }
}
