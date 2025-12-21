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

    @Test
    void getEnrollmentsReturnsServiceData() {
        when(analyticsService.getEnrollmentsOverview("daily", null, null))
                .thenReturn(List.of(Map.of("count", 1)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getEnrollments("daily", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getCompletionRatesReturnsServiceData() {
        when(analyticsService.getCompletionRates()).thenReturn(List.of(Map.of("courseId", "c1")));

        ResponseEntity<List<Map<String, Object>>> response = controller.getCompletionRates();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getCertificatesReturnsServiceData() {
        when(analyticsService.getCertificatesIssuedOverview("monthly", null, null))
                .thenReturn(List.of(Map.of("count", 2)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getCertificates("monthly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getUserGrowthReturnsServiceData() {
        when(analyticsService.getUserGrowth("monthly", null, null))
                .thenReturn(List.of(Map.of("count", 3)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getUserGrowth("monthly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getRolesBreakdownReturnsServiceData() {
        when(analyticsService.getUserRolesBreakdown()).thenReturn(Map.of("ADMIN", 1L));

        ResponseEntity<Map<String, Long>> response = controller.getRolesBreakdown();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("ADMIN", 1L);
    }

    @Test
    void getGeoBreakdownReturnsServiceData() {
        when(analyticsService.getUserGeoBreakdown()).thenReturn(Map.of("TN", 2L));

        ResponseEntity<Map<String, Long>> response = controller.getGeoBreakdown();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("TN", 2L);
    }

    @Test
    void getNewRegistrationsReturnsServiceData() {
        when(analyticsService.getNewRegistrations("weekly", null, null))
                .thenReturn(List.of(Map.of("count", 4)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getNewRegistrations("weekly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getLatestUserReturnsServiceData() {
        when(analyticsService.getLatestUserSummary()).thenReturn(Map.of("latestUser", "u1"));

        ResponseEntity<Map<String, Object>> response = controller.getLatestUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("latestUser", "u1");
    }

    @Test
    void getPostsTrendReturnsServiceData() {
        when(analyticsService.getPostsTrend("weekly", null, null))
                .thenReturn(List.of(Map.of("count", 1)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getPostsTrend("weekly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getTopPostsReturnsServiceData() {
        when(analyticsService.getTopPostsByEngagement(10)).thenReturn(List.of(Map.of("postId", "p1")));

        ResponseEntity<List<Map<String, Object>>> response = controller.getTopPosts(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getCommentsTrendReturnsServiceData() {
        when(analyticsService.getCommentsTrend("weekly", null, null))
                .thenReturn(List.of(Map.of("count", 1)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getCommentsTrend("weekly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getEngagementAveragesReturnsServiceData() {
        when(analyticsService.getUserEngagementAverages()).thenReturn(Map.of("avgLikesPerUser", 1.0));

        ResponseEntity<Map<String, Object>> response = controller.getEngagementAverages();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("avgLikesPerUser", 1.0);
    }

    @Test
    void getFeaturedPerformanceReturnsServiceData() {
        when(analyticsService.getFeaturedPostsPerformance()).thenReturn(Map.of("featuredAvgEngagement", 2.0));

        ResponseEntity<Map<String, Object>> response = controller.getFeaturedPerformance();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("featuredAvgEngagement", 2.0);
    }

    @Test
    void getNotificationsTrendReturnsServiceData() {
        when(analyticsService.getNotificationsTrend("weekly", null, null))
                .thenReturn(List.of(Map.of("count", 1)));

        ResponseEntity<List<Map<String, Object>>> response = controller.getNotificationsTrend("weekly", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getWebsocketActiveReturnsServiceData() {
        when(analyticsService.getWebSocketActivity()).thenReturn(Map.of("connectedUsers", 1L));

        ResponseEntity<Map<String, Object>> response = controller.getWebsocketActive();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("connectedUsers", 1L);
    }

    @Test
    void getNotificationReadStatusReturnsServiceData() {
        when(analyticsService.getNotificationReadStatus()).thenReturn(Map.of("read", 1L, "unread", 2L, "total", 3L));

        ResponseEntity<Map<String, Object>> response = controller.getNotificationReadStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("total", 3L);
    }

    @Test
    void getTopNotificationTypesReturnsServiceData() {
        when(analyticsService.getTopNotificationTypes()).thenReturn(Map.of("COMMENT", 2L));

        ResponseEntity<Map<String, Long>> response = controller.getTopNotificationTypes();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("COMMENT", 2L);
    }
}
