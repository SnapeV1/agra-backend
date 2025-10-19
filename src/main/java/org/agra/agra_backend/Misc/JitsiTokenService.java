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

    @Value("${jitsi.token-ttl-seconds:3600}")
    private long tokenTtlSeconds;

    public String mintUserToken(User user, Session s, boolean moderator) {
        Instant now = Instant.now();

        Map<String, Object> userCtx = new HashMap<>();
        userCtx.put("name", user.getName());
        userCtx.put("id", user.getId());
        if (user.getEmail() != null) userCtx.put("email", user.getEmail());
        if (user.getPicture() != null) userCtx.put("avatar", user.getPicture());

        Map<String, Object> context = new HashMap<>();
        context.put("user", userCtx);
        context.put("moderator", moderator);

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setAudience("jitsi")
                .setIssuer(appId)
                .setSubject(sub)
                .claim("room", s.getRoomName())
                .claim("context", context)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(Duration.ofSeconds(tokenTtlSeconds))))
                .signWith(
                        Keys.hmacShaKeyFor(appSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}
