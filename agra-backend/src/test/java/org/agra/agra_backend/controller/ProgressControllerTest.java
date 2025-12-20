package org.agra.agra_backend.controller;

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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private ProgressController controller;

    @Test
    void updateLessonProgressReturnsBadRequestWhenCourseIdMissing() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", "lesson-1");

        ResponseEntity<?> response = controller.updateLessonProgress(payload, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
    void getCourseProgressReturnsUnauthorizedWhenMissingAuth() {
        ResponseEntity<?> response = controller.getCourseProgress("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
