package org.agra.agra_backend.controller;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService resetService;
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) throws MessagingException {
        String email = request.get("email");
        log.info("POST /api/auth/forgot-password - email={} (normalized internally)", email);
        resetService.initiateReset(email);
        return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a reset link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        log.info("POST /api/auth/reset-password - tokenPresent={} passwordPresent={}",
                token != null && !token.isBlank(), password != null && !password.isBlank());
        try {
            resetService.resetPassword(token, password);
            return ResponseEntity.ok(Map.of("message", "Password reset successful."));
        } catch (RuntimeException e) {
            log.warn("POST /api/auth/reset-password - failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Alias endpoint to support clients calling /set-password instead of /reset-password
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        log.info("POST /api/auth/set-password - tokenPresent={} passwordPresent={}",
                token != null && !token.isBlank(), password != null && !password.isBlank());
        try {
            resetService.resetPassword(token, password);
            return ResponseEntity.ok(Map.of("message", "Password set successfully."));
        } catch (RuntimeException e) {
            log.warn("POST /api/auth/set-password - failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
