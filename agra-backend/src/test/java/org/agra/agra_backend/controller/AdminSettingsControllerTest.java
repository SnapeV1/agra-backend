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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
}
