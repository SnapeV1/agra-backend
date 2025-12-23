package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.CourseProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseProgressServiceTest {

    @Mock
    private CourseProgressRepository courseProgressRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private CourseProgressService service;

    @Test
    void enrollUserReturnsExistingWhenAlreadyEnrolled() {
        CourseProgress existing = new CourseProgress();
        existing.setUserId("user-1");
        existing.setCourseId("course-1");
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(existing));

        CourseProgress result = service.enrollUserInCourse("user-1", "course-1");

        assertThat(result).isSameAs(existing);
        verify(courseProgressRepository, never()).save(any(CourseProgress.class));
    }

    @Test
    void enrollUserCreatesProgressWhenMissing() {
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.empty());
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress created = service.enrollUserInCourse("user-1", "course-1");

        assertThat(created.getUserId()).isEqualTo("user-1");
        assertThat(created.getCourseId()).isEqualTo("course-1");
        assertThat(created.isCompleted()).isFalse();
        assertThat(created.getProgressPercentage()).isZero();
        assertThat(created.getEnrolledAt()).isNotNull();
        verify(activityLogService).logUserActivity(
                "user-1",
                ActivityType.COURSE_ENROLLMENT,
                "Enrolled in course",
                "COURSE",
                "course-1",
                Map.of("courseId", "course-1")
        );
    }

    @Test
    void isUserEnrolledDelegatesToRepository() {
        when(courseProgressRepository.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(true);

        assertThat(service.isUserEnrolledInCourse("user-1", "course-1")).isTrue();
    }

    @Test
    void getEnrollmentStatusReturnsEmptyWhenMissing() {
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.empty());

        assertThat(service.getEnrollmentStatus("user-1", "course-1")).isEmpty();
    }

    @Test
    void getUserEnrollmentsDelegates() {
        when(courseProgressRepository.findByUserId("user-1")).thenReturn(List.of(new CourseProgress()));

        assertThat(service.getUserEnrollments("user-1")).hasSize(1);
    }

    @Test
    void getCourseEnrollmentsDelegates() {
        when(courseProgressRepository.findByCourseId("course-1")).thenReturn(List.of(new CourseProgress()));

        assertThat(service.getCourseEnrollments("course-1")).hasSize(1);
    }

    @Test
    void updateProgressSetsCompletedWhen100() {
        CourseProgress progress = new CourseProgress();
        progress.setProgressPercentage(10);
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.updateProgress("user-1", "course-1", 100);

        assertThat(updated.getProgressPercentage()).isEqualTo(100);
        assertThat(updated.isCompleted()).isTrue();
        verify(activityLogService).logUserActivity(
                "user-1",
                ActivityType.COURSE_COMPLETION,
                "Completed course",
                "COURSE",
                "course-1",
                Map.of("courseId", "course-1")
        );
    }

    @Test
    void updateProgressSkipsLoggingWhenAlreadyCompleted() {
        CourseProgress progress = new CourseProgress();
        progress.setProgressPercentage(100);
        progress.setCompleted(true);
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.updateProgress("user-1", "course-1", 100);

        assertThat(updated.isCompleted()).isTrue();
        verify(activityLogService, never()).logUserActivity(
                anyString(),
                any(ActivityType.class),
                anyString(),
                anyString(),
                anyString(),
                anyMap()
        );
    }

    @Test
    void updateProgressThrowsWhenMissing() {
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProgress("user-1", "course-1", 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void unenrollDeletesWhenPresent() {
        CourseProgress progress = new CourseProgress();
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));

        service.unenrollUser("user-1", "course-1");

        verify(courseProgressRepository).delete(progress);
    }

    @Test
    void markLessonCompleteInitializesCollections() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setCourseId("course-1");
        progress.setCompletedLessons(null);
        progress.setLessonCompletionDates(null);
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.markLessonComplete("user-1", "course-1", "lesson-1", new Date());

        assertThat(updated.getCompletedLessons()).contains("lesson-1");
        assertThat(updated.getLessonCompletionDates()).containsKey("lesson-1");
        assertThat(updated.getStartedAt()).isNotNull();
    }

    @Test
    void markLessonCompleteSkipsDuplicateLesson() {
        CourseProgress progress = new CourseProgress();
        progress.setCompletedLessons(new java.util.ArrayList<>(List.of("lesson-1")));
        progress.setLessonCompletionDates(new java.util.HashMap<>());
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.markLessonComplete("user-1", "course-1", "lesson-1", new Date());

        assertThat(updated.getCompletedLessons()).containsExactly("lesson-1");
    }

    @Test
    void markLessonCompleteThrowsWhenMissing() {
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markLessonComplete("user-1", "course-1", "lesson-1", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void setCurrentLessonSetsStartedAt() {
        CourseProgress progress = new CourseProgress();
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.setCurrentLesson("user-1", "course-1", "lesson-1");

        assertThat(updated.getCurrentLessonId()).isEqualTo("lesson-1");
        assertThat(updated.getStartedAt()).isNotNull();
    }

    @Test
    void setCurrentLessonThrowsWhenNotEnrolled() {
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setCurrentLesson("user-1", "course-1", "lesson-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void markCourseCompleteSetsCompletionAndCertificate() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setCourseId("course-1");
        progress.setCertificateUrl(null);
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.markCourseComplete("user-1", "course-1", new Date());

        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getProgressPercentage()).isEqualTo(100);
        assertThat(updated.getCertificateUrl()).contains("course-1").contains("user-1");
        assertThat(updated.getStartedAt()).isNotNull();
        verify(activityLogService).logUserActivity(
                "user-1",
                ActivityType.COURSE_COMPLETION,
                "Completed course",
                "COURSE",
                "course-1",
                Map.of("courseId", "course-1")
        );
    }

    @Test
    void markCourseCompleteKeepsExistingCertificate() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setCourseId("course-1");
        progress.setCertificateUrl("existing");
        progress.setStartedAt(new Date());
        when(courseProgressRepository.findByUserIdAndCourseId("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress updated = service.markCourseComplete("user-1", "course-1", new Date());

        assertThat(updated.getCertificateUrl()).isEqualTo("existing");
    }

    @Test
    void cleanupOrphanedEnrollmentsDeletesMissingCourses() {
        CourseProgress progress1 = new CourseProgress();
        progress1.setCourseId("course-1");
        progress1.setUserId("user-1");
        CourseProgress progress2 = new CourseProgress();
        progress2.setCourseId("course-2");
        progress2.setUserId("user-2");
        when(courseProgressRepository.findAll()).thenReturn(List.of(progress1, progress2));

        CourseService courseService = mock(CourseService.class);
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());
        when(courseService.getCourseById("course-2")).thenReturn(Optional.of(new org.agra.agra_backend.model.Course()));

        int deleted = service.cleanupOrphanedEnrollments(courseService);

        assertThat(deleted).isEqualTo(1);
        verify(courseProgressRepository).delete(progress1);
    }

    @Test
    void cleanupOrphanedEnrollmentsIgnoresErrors() {
        CourseProgress progress = new CourseProgress();
        progress.setCourseId("course-1");
        progress.setUserId("user-1");
        when(courseProgressRepository.findAll()).thenReturn(List.of(progress));

        CourseService courseService = mock(CourseService.class);
        when(courseService.getCourseById("course-1")).thenThrow(new RuntimeException("boom"));

        int deleted = service.cleanupOrphanedEnrollments(courseService);

        assertThat(deleted).isZero();
        verify(courseProgressRepository, never()).delete(any(CourseProgress.class));
    }
}
