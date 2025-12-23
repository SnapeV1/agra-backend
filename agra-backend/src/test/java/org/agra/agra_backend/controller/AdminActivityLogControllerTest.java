package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.ActivityLogService;
import org.agra.agra_backend.service.AdminAuditLogService;
import org.agra.agra_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActivityLogControllerTest {

    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private AdminAuditLogService adminAuditLogService;
    @Mock
    private UserService userService;

    private AdminActivityLogController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminActivityLogController(activityLogService, adminAuditLogService, userService, 31);
    }

    @Test
    void listActivityLogsDelegatesToService() {
        ActivityLog log = new ActivityLog();
        log.setId("a1");
        when(activityLogService.searchForAdmin("u1", ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                50)).thenReturn(List.of(log));
        User admin = new User();
        admin.setId("admin-1");
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);

        List<ActivityLog> result = controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                "audit-check",
                50
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("a1");
        verify(activityLogService).searchForAdmin("u1", ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                50);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass((Class) Map.class);
        verify(adminAuditLogService).logAccess(eq(admin), eq("ACTIVITY_LOG_QUERY"), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).containsEntry("reason", "audit-check");
    }

    @Test
    void listActivityLogsRequiresUserId() {
        assertThatThrownBy(() -> controller.listActivityLogs(
                " ",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                "audit-check",
                50
        )).hasMessageContaining("userId is required");
    }

    @Test
    void listActivityLogsRequiresBothStartAndEnd() {
        assertThatThrownBy(() -> controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                null,
                LocalDateTime.of(2025, 1, 2, 0, 0),
                "audit-check",
                50
        )).hasMessageContaining("start and end must both be provided");
    }

    @Test
    void listActivityLogsAllowsMissingDateRange() {
        when(activityLogService.searchForAdmin("u1", ActivityType.LIKE, null, null, 50))
                .thenReturn(List.of());
        User admin = new User();
        admin.setId("admin-1");
        when(userService.getCurrentUserOrThrow()).thenReturn(admin);

        List<ActivityLog> result = controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                null,
                null,
                "audit-check",
                50
        );

        assertThat(result).isEmpty();
        verify(activityLogService).searchForAdmin("u1", ActivityType.LIKE, null, null, 50);
    }

    @Test
    void listActivityLogsRequiresReason() {
        assertThatThrownBy(() -> controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                " ",
                50
        )).hasMessageContaining("reason is required");
    }

    @Test
    void listActivityLogsRejectsInvalidWindow() {
        assertThatThrownBy(() -> controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 2, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0),
                "audit-check",
                50
        )).hasMessageContaining("end must be after start");
    }

    @Test
    void listActivityLogsRejectsTooLargeWindow() {
        assertThatThrownBy(() -> controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 2, 15, 0, 0),
                "audit-check",
                50
        )).hasMessageContaining("date window exceeds");
    }
}
