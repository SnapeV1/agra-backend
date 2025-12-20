package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseLikeService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseService;
import org.agra.agra_backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private CourseService courseService;
    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CourseLikeService courseLikeService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CourseController controller;

    @Test
    void enrollInCourseReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.enrollInCourse("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enrollInCourseReturnsNotFoundWhenCourseMissing() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.enrollInCourse("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enrollInCourseReturnsCreatedOnSuccess() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);

        when(courseService.getCourseById("course-1")).thenReturn(Optional.of(new Course()));

        CourseProgress progress = new CourseProgress();
        progress.setEnrolledAt(new Date());
        progress.setProgressPercentage(25);
        when(courseProgressService.enrollUserInCourse("user-1", "course-1")).thenReturn(progress);

        ResponseEntity<Object> response = controller.enrollInCourse("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object body = response.getBody();
        assertThat(body).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) body;
        assertThat(payload).containsEntry("enrolled", true);
    }
}
