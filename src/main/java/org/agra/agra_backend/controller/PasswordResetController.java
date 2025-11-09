package org.agra.agra_backend.controller;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) throws MessagingException {
        String email = request.get("email");
        resetService.initiateReset(email);
        return ResponseEntity.ok(Map.of("message", "If your email is registered, you will receive a reset link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        resetService.resetPassword(token, password);
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }
}

