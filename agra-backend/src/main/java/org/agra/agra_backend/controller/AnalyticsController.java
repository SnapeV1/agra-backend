package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/courses/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCourseSummary() {
        log.info("GET /api/analytics/courses/summary - request received");
        Map<String, Object> summary = analyticsService.getCourseStatusSummary();
        log.info("GET /api/analytics/courses/summary - response: {}", summary);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getEnrollments(
            @RequestParam(defaultValue = "daily") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/enrollments - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getEnrollmentsOverview(granularity, start, end);
        log.info("GET /api/analytics/enrollments - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/top-courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTopCourses(
            @RequestParam(defaultValue = "enrollments") String metric,
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("GET /api/analytics/top-courses - metric={}, limit={}", metric, limit);
        List<Map<String, Object>> data = analyticsService.getTopCourses(metric, limit);
        log.info("GET /api/analytics/top-courses - returned={} items", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/completion-rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCompletionRates() {
        log.info("GET /api/analytics/completion-rates - request received");
        List<Map<String, Object>> data = analyticsService.getCompletionRates();
        log.info("GET /api/analytics/completion-rates - courses={} entries", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCertificates(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/certificates - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getCertificatesIssuedOverview(granularity, start, end);
        log.info("GET /api/analytics/certificates - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    // ===== User Analytics =====
    @GetMapping("/users/growth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUserGrowth(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/users/growth - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getUserGrowth(granularity, start, end);
        log.info("GET /api/analytics/users/growth - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/users/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getActiveUsers(@RequestParam(defaultValue = "30") int days) {
        log.info("GET /api/analytics/users/active - windowDays={}", days);
        Map<String, Object> out = analyticsService.getActiveVsInactiveUsers(days);
        log.info("GET /api/analytics/users/active - active={}, inactive={}, total={}", out.get("active"), out.get("inactive"), out.get("total"));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/users/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getRolesBreakdown() {
        log.info("GET /api/analytics/users/roles - request received");
        Map<String, Long> data = analyticsService.getUserRolesBreakdown();
        log.info("GET /api/analytics/users/roles - buckets={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/users/geo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getGeoBreakdown() {
        log.info("GET /api/analytics/users/geo - request received");
        Map<String, Long> data = analyticsService.getUserGeoBreakdown();
        log.info("GET /api/analytics/users/geo - countries={} buckets", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/users/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getNewRegistrations(
            @RequestParam(defaultValue = "weekly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/users/registrations - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getNewRegistrations(granularity, start, end);
        log.info("GET /api/analytics/users/registrations - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    // ===== Social / Feed Analytics =====
    @GetMapping("/social/posts/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPostsTrend(
            @RequestParam(defaultValue = "weekly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/social/posts/trend - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getPostsTrend(granularity, start, end);
        log.info("GET /api/analytics/social/posts/trend - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/social/posts/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTopPosts(@RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/analytics/social/posts/top - limit={}", limit);
        List<Map<String, Object>> data = analyticsService.getTopPostsByEngagement(limit);
        log.info("GET /api/analytics/social/posts/top - returned={} items", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/social/comments/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCommentsTrend(
            @RequestParam(defaultValue = "weekly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/social/comments/trend - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getCommentsTrend(granularity, start, end);
        log.info("GET /api/analytics/social/comments/trend - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/social/engagement/averages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEngagementAverages() {
        log.info("GET /api/analytics/social/engagement/averages - request received");
        Map<String, Object> out = analyticsService.getUserEngagementAverages();
        log.info("GET /api/analytics/social/engagement/averages - avgLikesPerUser={}, avgCommentsPerUser={}", out.get("avgLikesPerUser"), out.get("avgCommentsPerUser"));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/social/featured-performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeaturedPerformance() {
        log.info("GET /api/analytics/social/featured-performance - request received");
        Map<String, Object> out = analyticsService.getFeaturedPostsPerformance();
        log.info("GET /api/analytics/social/featured-performance - featuredAvgEngagement={}, regularAvgEngagement={}", out.get("featuredAvgEngagement"), out.get("regularAvgEngagement"));
        return ResponseEntity.ok(out);
    }

    // ===== Notifications & Activity =====
    @GetMapping("/notifications/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getNotificationsTrend(
            @RequestParam(defaultValue = "weekly") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date end
    ) {
        log.info("GET /api/analytics/notifications/trend - granularity={}, start={}, end={}", granularity, start, end);
        List<Map<String, Object>> data = analyticsService.getNotificationsTrend(granularity, start, end);
        log.info("GET /api/analytics/notifications/trend - points={}", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/notifications/websocket/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getWebsocketActive() {
        log.info("GET /api/analytics/notifications/websocket/active - request received");
        Map<String, Object> out = analyticsService.getWebSocketActivity();
        log.info("GET /api/analytics/notifications/websocket/active - connectedUsers(sessions)={} onlineUsers={}", out.get("connectedUsers"), out.get("onlineUsers"));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/notifications/read-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNotificationReadStatus() {
        log.info("GET /api/analytics/notifications/read-status - request received");
        Map<String, Object> out = analyticsService.getNotificationReadStatus();
        log.info("GET /api/analytics/notifications/read-status - read={}, unread={}, total={}", out.get("read"), out.get("unread"), out.get("total"));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/notifications/top-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getTopNotificationTypes() {
        log.info("GET /api/analytics/notifications/top-types - request received");
        Map<String, Long> data = analyticsService.getTopNotificationTypes();
        log.info("GET /api/analytics/notifications/top-types - types={} buckets", data.size());
        return ResponseEntity.ok(data);
    }
}
