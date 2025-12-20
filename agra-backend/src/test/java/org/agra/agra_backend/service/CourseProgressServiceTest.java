package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.model.CourseProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseProgressServiceTest {

    @Mock
    private CourseProgressRepository courseProgressRepository;

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
        assertThat(created.getProgressPercentage()).isEqualTo(0);
        assertThat(created.getEnrolledAt()).isNotNull();
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
    }
}
