package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.model.NotificationPreferences;
import org.agra.agra_backend.service.NotificationService;
import org.agra.agra_backend.service.NotificationPreferencesService;
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
    private final NotificationPreferencesService preferencesService;

    public NotificationController(NotificationService notificationService,
                                  NotificationPreferencesService preferencesService) {
        this.notificationService = notificationService;
        this.preferencesService = preferencesService;
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
    public ResponseEntity<?> getMyPreferences(Authentication authentication) {
        if (!isUserAuthenticated(authentication)) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        String userId = ((User) authentication.getPrincipal()).getId();
        NotificationPreferences prefs = preferencesService.getOrCreate(userId);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/preferences/me")
    public ResponseEntity<?> updateMyPreferences(@RequestBody NotificationPreferences request, Authentication authentication) {
        if (!isUserAuthenticated(authentication)) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        String userId = ((User) authentication.getPrincipal()).getId();
        NotificationPreferences saved = preferencesService.upsert(userId, request);
        return ResponseEntity.ok(saved);
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

    private boolean isUserAuthenticated(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof User;
    }
}
