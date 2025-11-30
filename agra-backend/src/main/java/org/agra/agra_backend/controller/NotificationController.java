package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> getAll() {
        return notificationService.getAllNotifications();
    }

    @GetMapping("/me")
    public List<Notification> getAllForCurrentUser() {
        String userId = getCurrentUserId();
        return notificationService.getAllForUser(userId);
    }

    @GetMapping("/unseen")
    public List<Notification> getUnseenForCurrentUser() {
        String userId = getCurrentUserId();
        return notificationService.getUnseenNotificationsForUser(userId);
    }

    @PostMapping("/{id}/seen")
    public void markSeen(@PathVariable("id") String notificationId) {
        String userId = getCurrentUserId();
        notificationService.markSeen(userId, notificationId);
    }

    @PostMapping("/mark-all-seen")
    public void markAllSeen() {
        String userId = getCurrentUserId();
        notificationService.markAllSeen(userId);
    }

    @GetMapping("/preferences/me")
    public ResponseEntity<Map<String, Object>> getMyPreferences(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        // Stubbed preferences response; adjust fields to match frontend expectations
        Map<String, Object> prefs = Map.of(
                "userId", user.getId(),
                "notificationsEnabled", true,
                "channels", List.of("email", "websocket"),
                "digest", Map.of("enabled", false, "frequency", "daily")
        );
        return ResponseEntity.ok(prefs);
    }

    @DeleteMapping({"", "/delete"})
    public void deleteAllForCurrentUser() {
        String userId = getCurrentUserId();
        notificationService.deleteAllForUser (userId);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof User u)) {
            throw new RuntimeException("Unauthorized");
        }
        return u.getId();
    }
}
