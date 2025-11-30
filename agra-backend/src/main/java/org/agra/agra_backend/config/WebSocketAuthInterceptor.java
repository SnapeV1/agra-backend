package org.agra.agra_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.agra.agra_backend.Misc.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Authenticates WebSocket handshakes using a JWT passed as a query param (?token=)
 * or Authorization header. Spring Security does not read query params automatically
 * for WebSocket upgrades, so we validate here and attach the Principal.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing token, uri={}", request.getURI());
            return false;
        }
        try {
            String userId = jwtUtil.extractUserId(token);
            if (userId == null || jwtUtil.isTokenExpired(token)) {
                log.warn("WebSocket handshake rejected: invalid/expired token, uri={}", request.getURI());
                return false;
            }
            // Attach principal so downstream messaging can identify the user
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            attributes.put(StompHeaderAccessor.USER_HEADER, auth);
            log.debug("WebSocket handshake accepted for userId={}, uri={}", userId, request.getURI());
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: {} uri={}", e.getMessage(), request.getURI());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, @Nullable Exception exception) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        // Query param ?token=
        URI uri = request.getURI();
        String query = uri.getRawQuery();
        if (query != null) {
            String tokenParam = null;
            String accessTokenParam = null;
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    String key = pair.substring(0, idx);
                    String value = pair.substring(idx + 1);
                    if (value == null || value.isBlank()) continue;
                    String decoded = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
                    if ("token".equalsIgnoreCase(key)) tokenParam = decoded;
                    if ("access_token".equalsIgnoreCase(key)) accessTokenParam = decoded;
                }
            }
            if (tokenParam != null) return tokenParam;
            if (accessTokenParam != null) return accessTokenParam;
        }

        // Authorization header Bearer
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpReq = servletRequest.getServletRequest();
            String header = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return null;
    }
}
