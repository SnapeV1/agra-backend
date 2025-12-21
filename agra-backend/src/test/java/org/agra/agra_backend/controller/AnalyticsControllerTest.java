package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController controller;

    @Test
    void getCourseSummaryReturnsServiceData() {
        when(analyticsService.getCourseStatusSummary()).thenReturn(Map.of("total", 1L));

        ResponseEntity<Map<String, Object>> response = controller.getCourseSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("total", 1L);
    }

    @Test
    void getTopCoursesReturnsServiceData() {
        when(analyticsService.getTopCourses("enrollments", 5)).thenReturn(List.of(Map.of("courseId", "c1")));

        ResponseEntity<List<Map<String, Object>>> response = controller.getTopCourses("enrollments", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getActiveUsersReturnsServiceData() {
        when(analyticsService.getActiveVsInactiveUsers(30)).thenReturn(Map.of("active", 1L));

        ResponseEntity<Map<String, Object>> response = controller.getActiveUsers(30);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("active", 1L);
    }
}
