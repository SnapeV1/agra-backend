package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.CertificateRecord;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CertificateService;
import org.agra.agra_backend.service.CourseProgressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private ProgressController controller;

    private Authentication authWithUser(String userId) {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(userId);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }

    private CourseProgress sampleProgress(String userId, String courseId) {
        CourseProgress progress = new CourseProgress();
        progress.setUserId(userId);
        progress.setCourseId(courseId);
        progress.setProgressPercentage(40);
        progress.setCompleted(false);
        progress.setStartedAt(new Date());
        progress.setCurrentLessonId("lesson-1");
        return progress;
    }

    @Test
    void updateLessonProgressReturnsBadRequestWhenCourseIdMissing() {
        Authentication authentication = authWithUser("user-1");

        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", "lesson-1");

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateLessonProgressReturnsUnauthorizedWhenMissingAuth() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        ResponseEntity<?> response = controller.updateLessonProgress(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateLessonProgressRejectsInvalidTimeSpent() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("timeSpent", "not-a-number");

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateLessonProgressRejectsNegativeTimeSpent() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("timeSpent", -5);

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateLessonProgressUpdatesCurrentLesson() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-99");
        payload.put("timeSpent", "12.5");

        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCurrentLessonId("lesson-99");
        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-99")).thenReturn(progress);

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("courseId", "course-1");
        assertThat(body).containsEntry("currentLessonId", "lesson-99");
    }

    @Test
    void updateLessonProgressRequiresEnrollmentWhenLessonIdMissing() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateLessonProgressReturnsForbiddenWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-1"))
                .thenThrow(new RuntimeException("not enrolled in course"));

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateLessonProgressReturnsServerErrorOnUnexpectedRuntime() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-1"))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void setCurrentLessonReturnsUnauthorizedWhenMissingAuth() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        ResponseEntity<?> response = controller.setCurrentLesson(payload, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void setCurrentLessonReturnsBadRequestWhenMissingPayload() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        ResponseEntity<?> response = controller.setCurrentLesson(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setCurrentLessonUpdatesLesson() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-2");

        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCurrentLessonId("lesson-2");
        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-2")).thenReturn(progress);

        ResponseEntity<?> response = controller.setCurrentLesson(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void setCurrentLessonReturnsForbiddenWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-2");

        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-2"))
                .thenThrow(new RuntimeException("not enrolled"));

        ResponseEntity<?> response = controller.setCurrentLesson(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void setCurrentLessonReturnsServerErrorOnRuntime() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-2");

        when(courseProgressService.setCurrentLesson("user-1", "course-1", "lesson-2"))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.setCurrentLesson(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getCourseProgressReturnsUnauthorizedWhenMissingAuth() {
        ResponseEntity<?> response = controller.getCourseProgress("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCourseProgressReturnsNotFoundWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getCourseProgress("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCourseProgressReturnsProgressDetails() {
        Authentication authentication = authWithUser("user-1");
        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompletedLessons(null);
        progress.setLessonCompletionDates(Map.of("lesson-1", new Date()));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));

        ResponseEntity<?> response = controller.getCourseProgress("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("courseId", "course-1");
        assertThat(body.get("completedLessons")).isInstanceOf(List.class);
    }

    @Test
    void getCourseProgressHandlesException() {
        Authentication authentication = authWithUser("user-1");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.getCourseProgress("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void markLessonCompleteReturnsBadRequestWhenMissingIds() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", "lesson-1");

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void markLessonCompleteReturnsBadRequestWhenLessonIdEmpty() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "  ");

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void markLessonCompleteReturnsForbiddenWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markLessonCompleteReturnsSuccess() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompletedLessons(new ArrayList<>(List.of("lesson-1")));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        when(courseProgressService.markLessonComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.eq("lesson-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(progress);

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("success", true);
    }

    @Test
    void markLessonCompleteReturnsForbiddenOnNotEnrolledRuntime() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(sampleProgress("user-1", "course-1")));
        when(courseProgressService.markLessonComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.eq("lesson-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenThrow(new RuntimeException("not enrolled"));

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markLessonCompleteReturnsServerErrorOnRuntime() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");
        payload.put("lessonId", "lesson-1");

        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(sampleProgress("user-1", "course-1")));
        when(courseProgressService.markLessonComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.eq("lesson-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.markLessonComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void markCourseCompleteReturnsBadRequestWhenMissingCourseId() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();

        ResponseEntity<?> response = controller.markCourseComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void markCourseCompleteIncludesCertificateRecordWhenIssued() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompleted(true);
        progress.setProgressPercentage(100);
        progress.setCertificateUrl("https://certs/test");
        CertificateRecord record = new CertificateRecord();
        record.setCertificateUrl("https://certs/test");
        record.setCertificateCode("CODE-1");
        record.setIssuedAt(new Date());

        when(courseProgressService.markCourseComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(progress);
        when(certificateService.recordIssuance(
                org.mockito.ArgumentMatchers.eq(progress),
                org.mockito.ArgumentMatchers.eq("https://certs/test"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(record);

        ResponseEntity<?> response = controller.markCourseComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("certificateCode", "CODE-1");
    }

    @Test
    void markCourseCompleteReturnsCertificateUrlWhenNoRecord() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompleted(true);
        progress.setProgressPercentage(100);
        progress.setCertificateUrl("https://certs/test");

        when(courseProgressService.markCourseComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(progress);
        when(certificateService.recordIssuance(
                org.mockito.ArgumentMatchers.eq(progress),
                org.mockito.ArgumentMatchers.eq("https://certs/test"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(null);

        ResponseEntity<?> response = controller.markCourseComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("certificateUrl", "https://certs/test");
    }

    @Test
    void markCourseCompleteReturnsForbiddenWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        when(courseProgressService.markCourseComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenThrow(new RuntimeException("not enrolled"));

        ResponseEntity<?> response = controller.markCourseComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void markCourseCompleteReturnsServerErrorOnRuntime() {
        Authentication authentication = authWithUser("user-1");
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", "course-1");

        when(courseProgressService.markCourseComplete(
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.any(Date.class)))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.markCourseComplete(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getCertificateReturnsUnauthorizedWhenMissingAuth() {
        ResponseEntity<?> response = controller.getCertificate("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCertificateReturnsNotFoundWhenNotEnrolled() {
        Authentication authentication = authWithUser("user-1");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCertificateReturnsBadRequestWhenNotCompleted() {
        Authentication authentication = authWithUser("user-1");
        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompleted(false);
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));

        ResponseEntity<?> response = controller.getCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCertificateReturnsNotFoundWhenMissingCertificateUrl() {
        Authentication authentication = authWithUser("user-1");
        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompleted(true);
        progress.setCertificateUrl(null);
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));

        ResponseEntity<?> response = controller.getCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCertificateReturnsPayloadWithRecord() {
        Authentication authentication = authWithUser("user-1");
        CourseProgress progress = sampleProgress("user-1", "course-1");
        progress.setCompleted(true);
        progress.setCertificateUrl("https://certs/test");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        CertificateRecord record = new CertificateRecord();
        record.setCertificateCode("CODE-2");
        record.setIssuedAt(new Date());
        when(certificateService.findByCourseAndUser("course-1", "user-1"))
                .thenReturn(Optional.of(record));

        ResponseEntity<?> response = controller.getCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("certificateCode", "CODE-2");
    }

    @Test
    void getCertificateHandlesException() {
        Authentication authentication = authWithUser("user-1");
        doThrow(new RuntimeException("boom"))
                .when(courseProgressService)
                .getEnrollmentStatus("user-1", "course-1");

        ResponseEntity<?> response = controller.getCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
