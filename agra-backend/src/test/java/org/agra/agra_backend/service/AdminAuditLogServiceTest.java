package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.AdminAuditLogRepository;
import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.AdminAuditLog;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    private AdminAuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditLogService(adminAuditLogRepository, 365);
    }

    @Test
    void logAccessStoresAdminInfo() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setName("Admin");
        admin.setEmail("admin@example.com");

        service.logAccess(admin, "ACTIVITY_LOG_QUERY", Map.of("userId", "u1"));

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isEqualTo("admin-1");
        assertThat(saved.getAction()).isEqualTo("ACTIVITY_LOG_QUERY");
        assertThat(saved.getMetadata()).containsEntry("userId", "u1");
        assertThat(saved.getAdminInfo().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void logAccessHandlesNullAdmin() {
        service.logAccess(null, "ACTIVITY_LOG_QUERY", Map.of());

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminUserId()).isNull();
        assertThat(saved.getAdminInfo()).isNull();
    }

    @Test
    void cleanupOldAuditLogsDeletesBeforeCutoff() {
        service.cleanupOldAuditLogs();
        verify(adminAuditLogRepository).deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void searchAsActivityLogsMapsAdminAuditLog() {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminUserId("admin-1");
        auditLog.setAction("ADMIN_PASSWORD_UPDATE");
        auditLog.setMetadata(Map.of("key", "value"));
        auditLog.setCreatedAt(LocalDateTime.of(2025, 1, 2, 0, 0));
        when(adminAuditLogRepository.findAll()).thenReturn(List.of(auditLog));

        List<ActivityLog> result = service.searchAsActivityLogs("admin-1", null, null, 10);

        assertThat(result).hasSize(1);
        ActivityLog mapped = result.get(0);
        assertThat(mapped.getUserId()).isEqualTo("admin-1");
        assertThat(mapped.getActivityType()).isEqualTo(ActivityType.ADMIN_ACTION);
        assertThat(mapped.getAction()).isEqualTo("ADMIN_PASSWORD_UPDATE");
        assertThat(mapped.getMetadata()).containsEntry("key", "value");
    }
}
