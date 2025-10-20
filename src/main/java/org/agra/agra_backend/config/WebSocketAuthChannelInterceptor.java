package org.agra.agra_backend.config;

import org.agra.agra_backend.Misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public WebSocketAuthChannelInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token != null && !token.isBlank()) {
                try {
                    String userId = jwtUtil.extractUserId(token);
                    if (userId != null) {
                        // Principal name must match convertAndSendToUser target; use userId as the name
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId, null, java.util.Collections.emptyList());
                        accessor.setUser(authentication);
                    }
                } catch (Exception e) {
                    log.debug("WebSocket CONNECT token rejected: {}", e.getMessage());
                }
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        // Try Authorization header
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            String header = auth.get(0);
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return header;
        }
        // Fallback: token or access_token headers
        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.get(0);
        }
        List<String> accessToken = accessor.getNativeHeader("access_token");
        if (accessToken != null && !accessToken.isEmpty()) {
            return accessToken.get(0);
        }
        return null;
    }
}

