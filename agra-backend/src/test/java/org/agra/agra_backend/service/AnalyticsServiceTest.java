package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CommentRepository;
import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.NotificationStatusRepository;
import org.agra.agra_backend.dao.PostRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Comment;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.Like;
import org.agra.agra_backend.model.NotificationStatus;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseProgressRepository courseProgressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationStatusRepository notificationStatusRepository;
    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private AnalyticsService service;

    @Test
    void getCourseStatusSummaryCountsArchived() {
        Course c1 = new Course();
        c1.setId("c1");
        c1.setArchived(false);
        Course c2 = new Course();
        c2.setId("c2");
        c2.setArchived(true);
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        Map<String, Object> summary = service.getCourseStatusSummary();

        assertThat(summary).containsEntry("total", 2L);
        assertThat(summary).containsEntry("archived", 1L);
        assertThat(summary).containsEntry("published", 1L);
    }

    @Test
    void getTopCoursesSortsByEnrollments() {
        Course c1 = new Course();
        c1.setId("c1");
        c1.setTitle("A");
        Course c2 = new Course();
        c2.setId("c2");
        c2.setTitle("B");
        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        CourseProgress cp1 = new CourseProgress();
        cp1.setCourseId("c1");
        CourseProgress cp2 = new CourseProgress();
        cp2.setCourseId("c1");
        CourseProgress cp3 = new CourseProgress();
        cp3.setCourseId("c2");
        when(courseProgressRepository.findAll()).thenReturn(List.of(cp1, cp2, cp3));

        List<Map<String, Object>> result = service.getTopCourses("enrollments", 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("courseId", "c1");
    }

    @Test
    void getActiveVsInactiveUsersCountsActivity() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setEnrolledAt(new Date());
        when(courseProgressRepository.findAll()).thenReturn(List.of(progress));
        when(postRepository.findAll()).thenReturn(List.of());
        when(commentRepository.findAll()).thenReturn(List.of());
        when(likeRepository.findAll()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(2L);

        Map<String, Object> result = service.getActiveVsInactiveUsers(30);

        assertThat(result).containsEntry("active", 1L);
        assertThat(result).containsEntry("inactive", 1L);
        assertThat(result).containsEntry("total", 2L);
    }

    @Test
    void getNotificationReadStatusCountsReadAndUnread() {
        NotificationStatus s1 = new NotificationStatus();
        s1.setSeen(true);
        NotificationStatus s2 = new NotificationStatus();
        s2.setSeen(false);
        when(notificationStatusRepository.findAll()).thenReturn(List.of(s1, s2));

        Map<String, Object> result = service.getNotificationReadStatus();

        assertThat(result).containsEntry("read", 1L);
        assertThat(result).containsEntry("unread", 1L);
        assertThat(result).containsEntry("total", 2L);
    }
}
