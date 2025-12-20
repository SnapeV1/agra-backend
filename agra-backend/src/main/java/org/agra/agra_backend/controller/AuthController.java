package org.agra.agra_backend.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.RegisterRequest;
import org.agra.agra_backend.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";

    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, UserService userService, AuthService authService, GoogleAuthService googleAuthService, EmailVerificationService emailVerificationService, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@RequestBody RegisterRequest request) {
        try {
            authService.registerUser(request);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "User registered successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        log.info("login phase");
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            String userId = claims.getSubject();
            Optional<User> userOptional = userRepository.findById(userId);

            return userOptional
                    .<ResponseEntity<Object>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Object> googleLogin(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            token = body.get("credential"); // Support GSI default field name
        }
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Missing Google ID token (token/credential)"));
        }
        org.agra.agra_backend.payload.LoginResponse response = googleAuthService.verifyGoogleToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(@RequestParam("token") String token) {
        try {
            emailVerificationService.verifyToken(token);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Email verified successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> resendVerification() {
        try {
            User user = userService.getCurrentUserOrThrow();
            emailVerificationService.sendVerificationEmail(user);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Verification email sent if your account is unverified."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get(KEY_REFRESH_TOKEN);
        try {
            var stored = refreshTokenService.validateRefreshToken(refreshToken);
            User user = userRepository.findById(stored.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found for refresh token"));

            String newAccess = jwtUtil.generateToken(user);
            String newRefresh = refreshTokenService.rotateRefreshToken(stored);
            return ResponseEntity.ok(Map.of(
                    "token", newAccess,
                    KEY_REFRESH_TOKEN, newRefresh,
                    "user", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestBody(required = false) Map<String, String> body) {
        String refreshToken = body == null ? null : body.get(KEY_REFRESH_TOKEN);
        // Revoke the presented refresh token if provided
        refreshTokenService.revokeByToken(refreshToken);
        // If an authenticated user is present, clear any stored tokens for that user
        try {
            User user = userService.getCurrentUserOrThrow();
            refreshTokenService.revokeAllForUser(user.getId());
        } catch (Exception ignored) {
            // If not authenticated, just rely on the provided refresh token revocation
        }
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Logged out"));
    }

    // Removed temporary "exists" endpoints in favor of flags on normal responses


}   
