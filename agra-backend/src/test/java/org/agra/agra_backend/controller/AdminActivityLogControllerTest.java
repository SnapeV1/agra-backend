package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.service.ActivityLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminActivityLogControllerTest {

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AdminActivityLogController controller;

    @Test
    void listActivityLogsDelegatesToService() {
        ActivityLog log = new ActivityLog();
        log.setId("a1");
        when(activityLogService.search("u1", ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                50)).thenReturn(List.of(log));

        List<ActivityLog> result = controller.listActivityLogs(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                50
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("a1");
        verify(activityLogService).search("u1", ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                50);
    }
}
