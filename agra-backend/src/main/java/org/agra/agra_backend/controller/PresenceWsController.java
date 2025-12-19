package org.agra.agra_backend.controller;

import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Handles WebSocket heartbeats to keep presence TTL fresh.
 */
@Controller
@RequiredArgsConstructor
public class PresenceWsController {

    private static final Logger log = LoggerFactory.getLogger(PresenceWsController.class);

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    @MessageMapping("/presence/heartbeat")
    @SendToUser("/queue/presence/heartbeat")
    public Map<String, Object> heartbeat(@Payload(required = false) Map<String, Object> payload,
                                         @Header("simpSessionId") String sessionId,
                                         Principal principal) {
        if (principal != null) {
            String userId = principal.getName();
            String username = resolveName(userId);
            presenceService.refresh(userId, sessionId);
            return Map.of("status", "ok", "userId", userId, "username", username);
        }
        return Map.of("status", "ok", "userId", "", "username", "");
    }

    private String resolveName(String userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("unknown");
    }
}
