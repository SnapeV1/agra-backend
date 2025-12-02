package org.agra.agra_backend.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.agra.agra_backend.model.AdminSettings;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.AdminSettingsService;
import org.agra.agra_backend.service.NewsService;
import org.agra.agra_backend.service.TwoFactorService;
import org.agra.agra_backend.service.UserService;
import org.agra.agra_backend.dao.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final NewsService newsService;
    private final TwoFactorService twoFactorService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSettingsController(AdminSettingsService adminSettingsService,
                                   NewsService newsService,
                                   TwoFactorService twoFactorService,
                                   UserService userService,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.adminSettingsService = adminSettingsService;
        this.newsService = newsService;
        this.twoFactorService = twoFactorService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ===================== News schedule + fetch-now ===================== */

    @GetMapping("/settings/news-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getNewsSchedule() {
        AdminSettings settings = newsService.getAdminSettings();
        Map<String, Object> resp = new HashMap<>();
        resp.put("cron", settings.getNewsCron());
        resp.put("cooldownSeconds", settings.getNewsFetchCooldownSeconds());
        resp.put("lastFetchAt", settings.getLastNewsFetchAt());
        return resp;
    }

    @PutMapping("/settings/news-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateNewsSchedule(@RequestBody Map<String, Object> body) {
        String cron = body.get("cron") == null ? null : body.get("cron").toString();
        if (!StringUtils.hasText(cron)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cron is required");
        }
        AdminSettings updated = newsService.updateNewsCron(cron);
        Map<String, Object> resp = new HashMap<>();
        resp.put("cron", updated.getNewsCron());
        return resp;
    }

    @PostMapping("/news/fetch-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchNow(@RequestBody(required = false) Map<String, Object> body) {
        Duration cooldownOverride = null;
        if (body != null && body.get("cooldownSeconds") instanceof Number n) {
            cooldownOverride = Duration.ofSeconds(n.longValue());
        }
        try {
            adminSettingsService.markNewsFetchNow(cooldownOverride);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
        }
        var articles = newsService.fetchNorthAfricaAgricultureNow();
        Map<String, Object> resp = new HashMap<>();
        resp.put("count", articles.size());
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    /* ===================== Security controls ===================== */

    @GetMapping("/settings/security")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getSecurity() {
        User admin = requireAdmin();
        Map<String, Object> resp = new HashMap<>();
        resp.put("twoFactorEnabled", Boolean.TRUE.equals(admin.getTwoFactorEnabled()));
        resp.put("email", admin.getEmail());
        return resp;
    }

    @PostMapping("/settings/security/2fa/enroll")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> enroll2fa() {
        User admin = requireAdmin();
        String secret = twoFactorService.generateSecret();
        List<String> recoveryCodes = twoFactorService.generateRecoveryCodes();
        admin.setTwoFactorEnabled(false);
        admin.setTwoFactorSecret(secret);
        admin.setTwoFactorRecoveryCodes(hashCodes(recoveryCodes));
        admin.setTwoFactorVerifiedAt(null);
        userRepository.save(admin);

        Map<String, Object> resp = new HashMap<>();
        resp.put("secret", secret);
        resp.put("otpauthUrl", twoFactorService.buildOtpAuthUrl(secret, admin.getEmail()));
        resp.put("recoveryCodes", recoveryCodes);
        return resp;
    }

    @PostMapping("/settings/security/2fa/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> body) {
        User admin = requireAdmin();
        String code = body.get("code");
        if (!twoFactorService.verifyCode(admin.getTwoFactorSecret(), code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid code"));
        }
        admin.setTwoFactorEnabled(true);
        admin.setTwoFactorVerifiedAt(new java.util.Date());
        userRepository.save(admin);
        return ResponseEntity.ok(Map.of("status", "2fa_enabled"));
    }

    @PutMapping("/settings/security/2fa/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disable2fa(@RequestBody Map<String, String> body) {
        User admin = requireAdmin();
        String currentPassword = body.get("currentPassword");
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Current password incorrect"));
        }
        String recoveryCode = body.get("recoveryCode");
        String totpCode = body.get("code");
        boolean totpOk = twoFactorService.verifyCode(admin.getTwoFactorSecret(), totpCode);
        boolean recoveryOk = recoveryCode != null && matchesRecoveryCode(admin, recoveryCode);
        if (!totpOk && !recoveryOk && Boolean.TRUE.equals(admin.getTwoFactorEnabled())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Valid TOTP or recovery code required"));
        }
        admin.setTwoFactorEnabled(false);
        admin.setTwoFactorSecret(null);
        admin.setTwoFactorRecoveryCodes(null);
        admin.setTwoFactorVerifiedAt(null);
        userRepository.save(admin);
        return ResponseEntity.ok(Map.of("status", "2fa_disabled"));
    }

    @PutMapping("/settings/security/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> body) {
        User admin = requireAdmin();
        String newEmail = body.get("email");
        String currentPassword = body.get("currentPassword");
        if (!StringUtils.hasText(newEmail)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email required"));
        }
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Current password incorrect"));
        }
        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Email already in use"));
        }
        admin.setEmail(newEmail);
        userRepository.save(admin);
        adminSettingsService.updateAdminEmail(newEmail);
        return ResponseEntity.ok(Map.of("status", "email_updated", "email", newEmail));
    }

    @PutMapping("/settings/security/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body) {
        User admin = requireAdmin();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Current password incorrect"));
        }
        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password must be at least 8 characters with upper, lower, digit and symbol"));
        }
        userService.changePassword(admin.getId(), currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("status", "password_updated"));
    }

    private boolean isStrongPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    private List<String> hashCodes(List<String> codes) {
        return codes.stream().map(passwordEncoder::encode).toList();
    }

    private boolean matchesRecoveryCode(User user, String candidate) {
        if (user.getTwoFactorRecoveryCodes() == null || candidate == null) return false;
        return user.getTwoFactorRecoveryCodes().stream().anyMatch(hash -> passwordEncoder.matches(candidate, hash));
    }

    private User requireAdmin() {
        User user = userService.getCurrentUserOrThrow();
        if (user.getRole() == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        return user;
    }
}
