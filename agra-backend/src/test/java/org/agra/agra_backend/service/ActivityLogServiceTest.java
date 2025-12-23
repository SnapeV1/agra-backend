package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.ActivityLogRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private UserRepository userRepository;

    private ActivityLogService service;

    @BeforeEach
    void setUp() {
        service = new ActivityLogService(activityLogRepository, userRepository, 180, "content,message");
    }

    @Test
    void logUserActivityWithUserStoresSnapshot() {
        User user = new User();
        user.setId("user-1");
        user.setName("User");
        user.setEmail("user@example.com");

        service.logUserActivity(user, ActivityType.LIKE, "Liked post", "POST", "post-1", Map.of("postId", "post-1"));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getActivityType()).isEqualTo(ActivityType.LIKE);
        assertThat(saved.getTargetType()).isEqualTo("POST");
        assertThat(saved.getTargetId()).isEqualTo("post-1");
        assertThat(saved.getMetadata()).containsEntry("postId", "post-1");
        assertThat(saved.getUserInfo().getName()).isEqualTo("User");
    }

    @Test
    void logUserActivityWithUserIdLoadsSnapshot() {
        User user = new User();
        user.setId("user-1");
        user.setName("User");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        service.logUserActivity("user-1", ActivityType.COURSE_ENROLLMENT, "Enrolled", "COURSE", "c1",
                Map.of("courseId", "c1"));

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertThat(saved.getUserInfo()).isNotNull();
        assertThat(saved.getUserInfo().getName()).isEqualTo("User");
    }

    @Test
    void logUserActivityWithNullUserSkipsUserInfo() {
        service.logUserActivity((User) null, ActivityType.LIKE, "Liked post", "POST", "post-1", null);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getUserInfo()).isNull();
    }

    @Test
    void searchFiltersAndSortsByCreatedAtDesc() {
        ActivityLog first = new ActivityLog();
        first.setId("a1");
        first.setUserId("u1");
        first.setActivityType(ActivityType.LIKE);
        first.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        ActivityLog second = new ActivityLog();
        second.setId("a2");
        second.setUserId("u1");
        second.setActivityType(ActivityType.COURSE_ENROLLMENT);
        second.setCreatedAt(LocalDateTime.of(2025, 1, 2, 9, 0));

        ActivityLog third = new ActivityLog();
        third.setId("a3");
        third.setUserId("u2");
        third.setActivityType(ActivityType.LIKE);
        third.setCreatedAt(LocalDateTime.of(2025, 1, 3, 8, 0));

        when(activityLogRepository.findAll()).thenReturn(List.of(first, second, third));

        List<ActivityLog> result = service.search(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 23, 59),
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("a1");
    }

    @Test
    void searchForAdminRedactsMetadataKeys() {
        ActivityLog log = new ActivityLog();
        log.setId("a1");
        log.setUserId("u1");
        log.setActivityType(ActivityType.LIKE);
        log.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        log.setMetadata(Map.of("content", "secret", "safe", "ok"));

        when(activityLogRepository.findAll()).thenReturn(List.of(log));

        List<ActivityLog> result = service.searchForAdmin(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).doesNotContainKey("content");
        assertThat(result.get(0).getMetadata()).containsEntry("safe", "ok");
    }

    @Test
    void searchForAdminKeepsMetadataWhenNoRedactionConfigured() {
        ActivityLog log = new ActivityLog();
        log.setId("a1");
        log.setUserId("u1");
        log.setActivityType(ActivityType.LIKE);
        log.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        log.setMetadata(Map.of("content", "secret", "safe", "ok"));

        when(activityLogRepository.findAll()).thenReturn(List.of(log));
        ActivityLogService noRedaction = new ActivityLogService(activityLogRepository, userRepository, 180, "");

        List<ActivityLog> result = noRedaction.searchForAdmin(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).containsEntry("content", "secret");
        assertThat(result.get(0).getMetadata()).containsEntry("safe", "ok");
    }

    @Test
    void searchForAdminReturnsEmptyMetadataWhenMissing() {
        ActivityLog log = new ActivityLog();
        log.setId("a1");
        log.setUserId("u1");
        log.setActivityType(ActivityType.LIKE);
        log.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        log.setMetadata(null);

        when(activityLogRepository.findAll()).thenReturn(List.of(log));

        List<ActivityLog> result = service.searchForAdmin(
                "u1",
                ActivityType.LIKE,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 2, 0, 0),
                10
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetadata()).isNotNull();
        assertThat(result.get(0).getMetadata()).isEmpty();
    }

    @Test
    void cleanupOldLogsDeletesBeforeCutoff() {
        service.cleanupOldLogs();

        verify(activityLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
