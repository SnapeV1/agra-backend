package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.NotificationPreferences;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.NotificationPreferencesService;
import org.agra.agra_backend.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPreferencesService preferencesService;

    @InjectMocks
    private NotificationController controller;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllReturnsServiceList() {
        when(notificationService.getAllNotifications()).thenReturn(List.of(new Notification()));

        List<Notification> result = controller.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllForCurrentUserRequiresAuth() {
        assertThatThrownBy(() -> controller.getAllForCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void getAllForCurrentUserUsesSecurityContext() {
        User user = new User();
        user.setId("user-1");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(notificationService.getAllForUser("user-1")).thenReturn(List.of());

        List<Notification> result = controller.getAllForCurrentUser();

        assertThat(result).isEmpty();
        verify(notificationService).getAllForUser("user-1");
    }

    @Test
    void markSeenUsesSecurityContext() {
        User user = new User();
        user.setId("user-1");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        controller.markSeen("notif-1");

        verify(notificationService).markSeen("user-1", "notif-1");
    }

    @Test
    void getMyPreferencesReturnsUnauthorized() {
        ResponseEntity<?> response = controller.getMyPreferences(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
    }

    @Test
    void updateMyPreferencesReturnsUnauthorized() {
        ResponseEntity<?> response = controller.updateMyPreferences(new NotificationPreferences(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
    }
}
