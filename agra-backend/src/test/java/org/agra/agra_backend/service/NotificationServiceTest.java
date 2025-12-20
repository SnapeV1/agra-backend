package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.NotificationStatusRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationStatusRepository notificationStatusRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService service;

    @Test
    void getUnseenNotificationsReturnsEmptyWhenNoStatuses() {
        when(notificationStatusRepository.findByUserIdAndSeenIsFalse("user-1"))
                .thenReturn(List.of());

        List<Notification> result = service.getUnseenNotificationsForUser("user-1");

        assertThat(result).isEmpty();
        verify(notificationRepository, never()).findAllById(any());
    }

    @Test
    void markSeenCreatesStatusWhenMissing() {
        when(notificationStatusRepository.findByUserIdAndNotificationId("user-1", "notif-1"))
                .thenReturn(Optional.empty());
        when(notificationStatusRepository.save(any(NotificationStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.markSeen("user-1", "notif-1");

        ArgumentCaptor<NotificationStatus> captor = ArgumentCaptor.forClass(NotificationStatus.class);
        verify(notificationStatusRepository).save(captor.capture());
        NotificationStatus saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getNotificationId()).isEqualTo("notif-1");
        assertThat(saved.isSeen()).isTrue();
        assertThat(saved.getSeenAt()).isNotNull();
    }

    @Test
    void markAllSeenCreatesStatusesForAllNotifications() {
        Notification n1 = new Notification();
        n1.setId("n1");
        Notification n2 = new Notification();
        n2.setId("n2");
        when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));
        when(notificationStatusRepository.findByUserId("user-1")).thenReturn(List.of());

        service.markAllSeen("user-1");

        ArgumentCaptor<List<NotificationStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationStatusRepository).saveAll(captor.capture());
        List<NotificationStatus> statuses = captor.getValue();
        assertThat(statuses).hasSize(2);
        assertThat(statuses).allMatch(NotificationStatus::isSeen);
        assertThat(statuses).allMatch(status -> status.getSeenAt() != null);
    }
}
