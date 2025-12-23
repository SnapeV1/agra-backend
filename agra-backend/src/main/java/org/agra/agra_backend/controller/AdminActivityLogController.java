package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.ActivityLogService;
import org.agra.agra_backend.service.AdminAuditLogService;
import org.agra.agra_backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activity-logs")
public class AdminActivityLogController {

    private final ActivityLogService activityLogService;
    private final AdminAuditLogService adminAuditLogService;
    private final UserService userService;
    private final int maxWindowDays;

    public AdminActivityLogController(ActivityLogService activityLogService,
                                      AdminAuditLogService adminAuditLogService,
                                      UserService userService,
                                      @Value("${activity.logs.max-window-days:31}") int maxWindowDays) {
        this.activityLogService = activityLogService;
        this.adminAuditLogService = adminAuditLogService;
        this.userService = userService;
        this.maxWindowDays = maxWindowDays;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ActivityLog> listActivityLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam String reason,
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        String resolvedUserId = resolveUserId(userId, email);
        validateFilters(resolvedUserId, start, end, reason);
        User admin = userService.getCurrentUserOrThrow();
        Map<String, Object> auditMetadata = new java.util.HashMap<>();
        auditMetadata.put("userId", resolvedUserId);
        auditMetadata.put("email", email);
        auditMetadata.put("activityType", activityType != null ? activityType.name() : null);
        auditMetadata.put("start", start);
        auditMetadata.put("end", end);
        auditMetadata.put("limit", limit);
        auditMetadata.put("reason", reason);
        adminAuditLogService.logAccess(admin, "ACTIVITY_LOG_QUERY", auditMetadata);
        return activityLogService.searchForAdmin(resolvedUserId, activityType, start, end, limit);
    }

    private void validateFilters(String userId, LocalDateTime start, LocalDateTime end, String reason) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "userId or email is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "reason is required");
        }
        if ((start == null) != (end == null)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "start and end must both be provided");
        }
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "end must be after start");
            }
            long windowDays = ChronoUnit.DAYS.between(start, end);
            if (windowDays > maxWindowDays) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "date window exceeds " + maxWindowDays + " days"
                );
            }
        }
    }

    private String resolveUserId(String userId, String email) {
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        if (email == null || email.isBlank()) {
            return null;
        }
        return userService.findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "No user found for email"
                ));
    }
}
