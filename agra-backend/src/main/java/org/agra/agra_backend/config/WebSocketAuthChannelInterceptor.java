package org.agra.agra_backend.config;

import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.service.PresenceService;
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
import org.springframework.lang.Nullable;

import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtUtil jwtUtil;
    private final PresenceService presenceService;

    public WebSocketAuthChannelInterceptor(JwtUtil jwtUtil, PresenceService presenceService) {
        this.jwtUtil = jwtUtil;
        this.presenceService = presenceService;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        refreshPresenceIfNeeded(accessor, command);
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                return;
            }
            // Principal name must match convertAndSendToUser target; use userId as the name
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, java.util.Collections.emptyList());
            accessor.setUser(authentication);
            presenceService.markOnline(userId, accessor.getSessionId());
        } catch (Exception e) {
            log.debug("WebSocket CONNECT token rejected: {}", e.getMessage());
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        java.security.Principal user = accessor.getUser();
        if (user == null) {
            return;
        }
        presenceService.markOfflineIfNoSessions(user.getName(), accessor.getSessionId());
    }

    private void refreshPresenceIfNeeded(StompHeaderAccessor accessor, StompCommand command) {
        java.security.Principal user = accessor.getUser();
        if (user == null) {
            return;
        }
        // Refresh TTL on activity and heartbeats
        if (command == null || StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.MESSAGE.equals(command)) {
            presenceService.refresh(user.getName(), accessor.getSessionId());
        }
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
