package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.service.ActivityLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/activity-logs")
public class AdminActivityLogController {

    private final ActivityLogService activityLogService;

    public AdminActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ActivityLog> listActivityLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        return activityLogService.search(userId, activityType, start, end, limit);
    }
}
