package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.AdminSettings;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.AdminSettingsService;
import org.agra.agra_backend.service.NewsService;
import org.agra.agra_backend.service.TwoFactorService;
import org.agra.agra_backend.service.UserService;
import org.agra.agra_backend.dao.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSettingsControllerTest {

    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private NewsService newsService;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSettingsController controller;

    private User adminUser() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        admin.setEmail("admin@example.com");
        admin.setPassword("hashed");
        return admin;
    }

    @Test
    void getNewsScheduleReturnsSettings() {
        AdminSettings settings = new AdminSettings("global");
        settings.setNewsCron("cron");
        settings.setNewsFetchCooldownSeconds(60);
        settings.setLastNewsFetchAt(Instant.now());
        when(newsService.getAdminSettings()).thenReturn(settings);

        Map<String, Object> response = controller.getNewsSchedule();

        assertThat(response).containsEntry("cron", "cron");
        assertThat(response).containsEntry("cooldownSeconds", 60);
    }

    @Test
    void fetchNowReturnsTooManyRequestsOnCooldown() {
        doThrow(new IllegalStateException("Rate limited")).when(adminSettingsService).markNewsFetchNow(null);

        ResponseEntity<Map<String, Object>> response = controller.fetchNow(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void fetchNowReturnsCountOnSuccess() {
        when(newsService.fetchNorthAfricaAgricultureNow()).thenReturn(List.of(new org.agra.agra_backend.model.NewsArticle()));

        ResponseEntity<Map<String, Object>> response = controller.fetchNow(Map.of("cooldownSeconds", 30));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("count", 1);
        verify(adminSettingsService).markNewsFetchNow(Duration.ofSeconds(30));
    }

    @Test
    void updateNewsScheduleRejectsBlankCron() {
        Map<String, Object> payload = Map.of();

        assertThatThrownBy(() -> controller.updateNewsSchedule(payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cron is required");
    }

    @Test
    void updateNewsScheduleUpdatesCron() {
        AdminSettings settings = new AdminSettings("global");
        settings.setNewsCron("0 0 9 ? * MON");
        when(newsService.updateNewsCron("0 0 9 ? * MON")).thenReturn(settings);

        Map<String, Object> response = controller.updateNewsSchedule(Map.of("cron", "0 0 9 ? * MON"));

        assertThat(response).containsEntry("cron", "0 0 9 ? * MON");
    }

    @Test
    void getSecurityRejectsNonAdmin() {
        User user = new User();
        user.setRole("USER");
        when(userService.getCurrentUserOrThrow()).thenReturn(user);

        assertThatThrownBy(controller::getSecurity)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Admin role required");
    }

    @Test
    void getSecurityReturnsAdminDetails() {
        User admin = adminUser();
        admin.setTwoFactorEnabled(true);
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);

        Map<String, Object> response = controller.getSecurity();

        assertThat(response).containsEntry("twoFactorEnabled", true);
        assertThat(response).containsEntry("email", "admin@example.com");
    }

    @Test
    void enroll2faReturnsSetupPayload() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(twoFactorService.generateSecret()).thenReturn("secret");
        when(twoFactorService.generateRecoveryCodes()).thenReturn(List.of("code1", "code2"));
        when(passwordEncoder.encode("code1")).thenReturn("hash1");
        when(passwordEncoder.encode("code2")).thenReturn("hash2");
        when(twoFactorService.buildOtpAuthUrl("secret", "admin@example.com")).thenReturn("otpauth://test");

        Map<String, Object> response = controller.enroll2fa();

        assertThat(response)
                .containsEntry("secret", "secret")
                .containsEntry("otpauthUrl", "otpauth://test")
                .containsEntry("recoveryCodes", List.of("code1", "code2"));
        assertThat(admin.getTwoFactorSecret()).isEqualTo("secret");
        verify(userRepository).save(admin);
    }

    @Test
    void verify2faRejectsInvalidCode() {
        User admin = adminUser();
        admin.setTwoFactorSecret("secret");
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(twoFactorService.verifyCode("secret", "123456")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.verify2fa(Map.of("code", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void verify2faEnablesTwoFactor() {
        User admin = adminUser();
        admin.setTwoFactorSecret("secret");
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(twoFactorService.verifyCode("secret", "123456")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.verify2fa(Map.of("code", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getTwoFactorEnabled()).isTrue();
        assertThat(admin.getTwoFactorVerifiedAt()).isNotNull();
        verify(userRepository).save(admin);
    }

    @Test
    void disable2faRejectsWrongPassword() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.disable2fa(Map.of("currentPassword", "bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void disable2faRejectsWithoutValidCodes() {
        User admin = adminUser();
        admin.setTwoFactorEnabled(true);
        admin.setTwoFactorSecret("secret");
        admin.setTwoFactorRecoveryCodes(List.of("hash"));
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(twoFactorService.verifyCode("secret", "000000")).thenReturn(false);
        when(passwordEncoder.matches("recovery", "hash")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.disable2fa(
                Map.of("currentPassword", "pass", "code", "000000", "recoveryCode", "recovery"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void disable2faSucceedsWithRecoveryCode() {
        User admin = adminUser();
        admin.setTwoFactorEnabled(true);
        admin.setTwoFactorSecret("secret");
        admin.setTwoFactorRecoveryCodes(List.of("hash"));
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(twoFactorService.verifyCode("secret", null)).thenReturn(false);
        when(passwordEncoder.matches("recovery", "hash")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.disable2fa(
                Map.of("currentPassword", "pass", "recoveryCode", "recovery"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getTwoFactorEnabled()).isFalse();
        assertThat(admin.getTwoFactorSecret()).isNull();
    }

    @Test
    void updateEmailRejectsDuplicateEmail() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        admin.setPassword("hashed");
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.updateEmail(
                Map.of("email", "admin@example.com", "currentPassword", "pass"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateEmailRejectsMissingEmail() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);

        ResponseEntity<Map<String, Object>> response = controller.updateEmail(
                Map.of("currentPassword", "pass"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateEmailRejectsWrongPassword() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.updateEmail(
                Map.of("email", "new@example.com", "currentPassword", "bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateEmailUpdatesAdminSettings() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.updateEmail(
                Map.of("email", "new@example.com", "currentPassword", "pass"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("email", "new@example.com");
        verify(userRepository).save(admin);
        verify(adminSettingsService).updateAdminEmail("new@example.com");
    }

    @Test
    void updatePasswordRejectsWrongCurrentPassword() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.updatePassword(
                Map.of("currentPassword", "bad", "newPassword", "Strong1!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updatePasswordRejectsWeakPassword() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.updatePassword(
                Map.of("currentPassword", "pass", "newPassword", "weak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updatePasswordUpdatesUser() {
        User admin = adminUser();
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.updatePassword(
                Map.of("currentPassword", "pass", "newPassword", "Strong1!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).changePassword("admin-1", "pass", "Strong1!");
    }
}
