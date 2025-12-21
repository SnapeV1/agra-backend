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
import org.agra.agra_backend.model.CourseTranslation;
import org.agra.agra_backend.model.Like;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.NotificationType;
import org.agra.agra_backend.model.NotificationStatus;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    void getCourseStatusSummaryHandlesEmptyList() {
        when(courseRepository.findAll()).thenReturn(List.of());

        Map<String, Object> summary = service.getCourseStatusSummary();

        assertThat(summary).containsEntry("total", 0L);
        assertThat(summary).containsEntry("archived", 0L);
        assertThat(summary).containsEntry("published", 0L);
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
    void getTopCoursesSortsByCompletionRateAndResolvesTranslations() {
        Course c1 = new Course();
        c1.setId("c1");
        c1.setTitle("");
        c1.setDefaultLanguage("fr");
        CourseTranslation fr = new CourseTranslation();
        fr.setTitle("Titre");
        fr.setDescription("Desc");
        fr.setGoals(List.of("goal"));
        c1.setTranslations(Map.of("fr", fr));

        Course c2 = new Course();
        c2.setId("c2");
        CourseTranslation en = new CourseTranslation();
        en.setTitle("English");
        en.setDescription("Desc");
        en.setGoals(List.of());
        c2.setTranslations(Map.of("en", en));

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        CourseProgress cp1 = new CourseProgress();
        cp1.setCourseId("c1");
        cp1.setCompleted(true);
        CourseProgress cp2 = new CourseProgress();
        cp2.setCourseId("c1");
        cp2.setCompleted(false);
        CourseProgress cp3 = new CourseProgress();
        cp3.setCourseId("c2");
        cp3.setCompleted(true);
        when(courseProgressRepository.findAll()).thenReturn(List.of(cp1, cp2, cp3));

        List<Map<String, Object>> result = service.getTopCourses("completionRate", 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("courseId", "c2");
    }

    @Test
    void getCompletionRatesIncludesMissingProgress() {
        Course c1 = new Course();
        c1.setId("c1");
        c1.setTitle("Course");
        when(courseRepository.findAll()).thenReturn(List.of(c1));

        CourseProgress cp = new CourseProgress();
        cp.setCourseId("other");
        cp.setCompleted(true);
        when(courseProgressRepository.findAll()).thenReturn(List.of(cp));

        List<Map<String, Object>> result = service.getCompletionRates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("completed", 0L);
        assertThat(result.get(0)).containsEntry("total", 0L);
    }

    @Test
    void getEnrollmentsOverviewBucketsByGranularity() {
        CourseProgress cp = new CourseProgress();
        Date enrolled = Date.from(ZonedDateTime.of(2025, 1, 8, 10, 0, 0, 0, ZoneOffset.UTC).toInstant());
        cp.setEnrolledAt(enrolled);
        when(courseProgressRepository.findAll()).thenReturn(List.of(cp));

        List<Map<String, Object>> weekly = service.getEnrollmentsOverview("weekly", null, null);
        List<Map<String, Object>> monthly = service.getEnrollmentsOverview("monthly", null, null);

        assertThat(weekly).hasSize(1);
        assertThat(monthly).hasSize(1);
    }

    @Test
    void getEnrollmentsOverviewAppliesDateFilters() {
        CourseProgress cp = new CourseProgress();
        Date enrolled = Date.from(ZonedDateTime.of(2025, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC).toInstant());
        cp.setEnrolledAt(enrolled);
        when(courseProgressRepository.findAll()).thenReturn(List.of(cp));

        Date start = Date.from(ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        List<Map<String, Object>> result = service.getEnrollmentsOverview("daily", start, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getCertificatesIssuedOverviewUsesCompletionDates() {
        CourseProgress cp1 = new CourseProgress();
        cp1.setCompleted(true);
        cp1.setCertificateUrl("url");
        cp1.setLessonCompletionDates(Map.of("l1", new Date(1000), "l2", new Date(2000)));

        CourseProgress cp2 = new CourseProgress();
        cp2.setCompleted(true);
        cp2.setCertificateUrl("");

        CourseProgress cp3 = new CourseProgress();
        cp3.setCompleted(true);
        cp3.setCertificateUrl("url2");
        cp3.setStartedAt(new Date(3000));

        CourseProgress cp4 = new CourseProgress();
        cp4.setCompleted(true);
        cp4.setCertificateUrl("url3");
        cp4.setEnrolledAt(new Date(4000));

        when(courseProgressRepository.findAll()).thenReturn(List.of(cp1, cp2, cp3, cp4));

        List<Map<String, Object>> result = service.getCertificatesIssuedOverview("daily", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("count", 3L);
    }

    @Test
    void getUserGrowthBuildsCumulativeSeries() {
        User u1 = new User();
        u1.setRegisteredAt(Date.from(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant()));
        User u2 = new User();
        u2.setRegisteredAt(Date.from(ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC).toInstant()));
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        Date start = Date.from(ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        List<Map<String, Object>> result = service.getUserGrowth("daily", start, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("count", 2L);
    }

    @Test
    void getUserRolesBreakdownFiltersNulls() {
        User u1 = new User();
        u1.setRole("ADMIN");
        User u2 = new User();
        u2.setRole(null);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        Map<String, Long> result = service.getUserRolesBreakdown();

        assertThat(result).containsEntry("ADMIN", 1L);
    }

    @Test
    void getUserGeoBreakdownFiltersBlank() {
        User u1 = new User();
        u1.setCountry("TN");
        User u2 = new User();
        u2.setCountry(" ");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        Map<String, Long> result = service.getUserGeoBreakdown();

        assertThat(result).containsEntry("TN", 1L);
    }

    @Test
    void getLatestUserSummaryHandlesEmpty() {
        when(userRepository.findTopByOrderByRegisteredAtDesc()).thenReturn(Optional.empty());

        Map<String, Object> result = service.getLatestUserSummary();

        assertThat(result).isEmpty();
    }

    @Test
    void getLatestUserSummaryIncludesUser() {
        User user = new User();
        user.setId("u1");
        user.setName("Name");
        user.setEmail("e@example.com");
        user.setRegisteredAt(new Date());
        when(userRepository.findTopByOrderByRegisteredAtDesc()).thenReturn(Optional.of(user));

        Map<String, Object> result = service.getLatestUserSummary();

        assertThat(result).containsKey("latestUser");
    }

    @Test
    void getNewRegistrationsReturnsBuckets() {
        User user = new User();
        user.setRegisteredAt(new Date(1000));
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<Map<String, Object>> result = service.getNewRegistrations("daily", null, null);

        assertThat(result).hasSize(1);
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
    void getActiveVsInactiveUsersAccountsForPostsCommentsLikes() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setStartedAt(new Date());
        when(courseProgressRepository.findAll()).thenReturn(List.of(progress));

        Post post = new Post();
        post.setUserId("user-2");
        post.setCreatedAt(LocalDateTime.now());
        when(postRepository.findAll()).thenReturn(List.of(post));

        Comment comment = new Comment();
        comment.setUserId("user-3");
        comment.setCreatedAt(LocalDateTime.now());
        when(commentRepository.findAll()).thenReturn(List.of(comment));

        Like like = new Like();
        like.setUserId("user-4");
        like.setCreatedAt(LocalDateTime.now());
        when(likeRepository.findAll()).thenReturn(List.of(like));

        when(userRepository.count()).thenReturn(10L);

        Map<String, Object> result = service.getActiveVsInactiveUsers(30);

        assertThat(result).containsEntry("active", 4L);
        assertThat(result).containsEntry("inactive", 6L);
    }

    @Test
    void getPostsTrendBucketsDates() {
        Post post = new Post();
        post.setCreatedAt(LocalDateTime.of(2025, 1, 5, 12, 0));
        when(postRepository.findAll()).thenReturn(List.of(post));

        List<Map<String, Object>> result = service.getPostsTrend("daily", null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCommentsTrendFiltersOutOfRange() {
        Comment comment = new Comment();
        comment.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
        when(commentRepository.findAll()).thenReturn(List.of(comment));

        Date start = Date.from(ZonedDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        List<Map<String, Object>> result = service.getCommentsTrend("daily", start, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getTopPostsByEngagementHandlesUserInfoAndLimit() {
        Post p1 = new Post();
        p1.setId("p1");
        p1.setLikesCount(null);
        p1.setCommentsCount(1L);
        p1.setIsCoursePost(true);

        Post p2 = new Post();
        p2.setId("p2");
        p2.setLikesCount(2L);
        p2.setCommentsCount(null);
        p2.setIsCoursePost(false);

        when(postRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Map<String, Object>> result = service.getTopPostsByEngagement(0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("postId", "p2");
    }

    @Test
    void getUserEngagementAveragesUsesDefaultUserCount() {
        when(likeRepository.findAll()).thenReturn(List.of());
        when(commentRepository.findAll()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(0L);

        Map<String, Object> result = service.getUserEngagementAverages();

        assertThat(result).containsEntry("avgLikesPerUser", 0.0);
        assertThat(result).containsEntry("avgCommentsPerUser", 0.0);
    }

    @Test
    void getFeaturedPostsPerformanceHandlesFeaturedOnly() {
        Post p1 = new Post();
        p1.setIsCoursePost(true);
        p1.setLikesCount(2L);
        p1.setCommentsCount(1L);
        when(postRepository.findAll()).thenReturn(List.of(p1));

        Map<String, Object> result = service.getFeaturedPostsPerformance();

        assertThat(result).containsEntry("featuredCount", 1L);
        assertThat(result).containsEntry("regularCount", 0L);
    }

    @Test
    void getFeaturedPostsPerformanceHandlesRegularOnly() {
        Post p1 = new Post();
        p1.setIsCoursePost(false);
        p1.setLikesCount(1L);
        p1.setCommentsCount(1L);
        when(postRepository.findAll()).thenReturn(List.of(p1));

        Map<String, Object> result = service.getFeaturedPostsPerformance();

        assertThat(result).containsEntry("featuredCount", 0L);
        assertThat(result).containsEntry("regularCount", 1L);
    }

    @Test
    void getNotificationsTrendBucketsDates() {
        Notification n = new Notification();
        n.setTimestamp(LocalDateTime.of(2025, 1, 5, 12, 0));
        when(notificationRepository.findAll()).thenReturn(List.of(n));

        List<Map<String, Object>> result = service.getNotificationsTrend("daily", null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getWebSocketActivityReturnsPresenceCounts() {
        when(presenceService.countOnlineSessions()).thenReturn(5L);
        when(presenceService.countOnlineUsers()).thenReturn(3L);

        Map<String, Object> result = service.getWebSocketActivity();

        assertThat(result).containsEntry("connectedUsers", 5L);
        assertThat(result).containsEntry("onlineUsers", 3L);
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

    @Test
    void getNotificationReadStatusHandlesEmpty() {
        when(notificationStatusRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = service.getNotificationReadStatus();

        assertThat(result).containsEntry("read", 0L);
        assertThat(result).containsEntry("unread", 0L);
        assertThat(result).containsEntry("total", 0L);
        assertThat(result).containsEntry("readPercentage", 0.0);
    }

    @Test
    void getTopNotificationTypesGroupsByType() {
        Notification n1 = new Notification();
        n1.setType(NotificationType.POST);
        Notification n2 = new Notification();
        n2.setType(null);
        when(notificationRepository.findAll()).thenReturn(List.of(n1, n2));

        Map<String, Long> result = service.getTopNotificationTypes();

        assertThat(result).containsEntry("POST", 1L);
    }
}
