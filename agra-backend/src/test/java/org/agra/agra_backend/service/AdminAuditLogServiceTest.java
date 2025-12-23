package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.AdminAuditLogRepository;
import org.agra.agra_backend.model.AdminAuditLog;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
}
