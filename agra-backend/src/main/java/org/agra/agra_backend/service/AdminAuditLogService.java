package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.AdminAuditLogRepository;
import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.AdminAuditLog;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final int retentionDays;

    public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository,
                                @Value("${admin.audit.retention-days:365}") int retentionDays) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.retentionDays = retentionDays;
    }

    public void logAccess(User admin, String action, Map<String, Object> metadata) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(admin != null ? admin.getId() : null);
        log.setAdminInfo(toUserInfo(admin));
        log.setAction(action);
        log.setMetadata(metadata);
        adminAuditLogRepository.save(log);
    }

    public List<ActivityLog> searchAsActivityLogs(String adminUserId,
                                                  LocalDateTime start,
                                                  LocalDateTime end,
                                                  Integer limit) {
        List<AdminAuditLog> all = adminAuditLogRepository.findAll();
        return all.stream()
                .filter(log -> adminUserId == null || adminUserId.equals(log.getAdminUserId()))
                .filter(log -> start == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(start)))
                .filter(log -> end == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(end)))
                .map(this::toActivityLog)
                .sorted(Comparator.comparing(ActivityLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit == null || limit < 1 ? Long.MAX_VALUE : limit)
                .toList();
    }

    @Scheduled(fixedDelayString = "${admin.audit.cleanup-interval-ms:86400000}")
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        adminAuditLogRepository.deleteByCreatedAtBefore(cutoff);
    }

    private ActivityLog toActivityLog(AdminAuditLog log) {
        ActivityLog out = new ActivityLog();
        out.setUserId(log.getAdminUserId());
        out.setUserInfo(log.getAdminInfo());
        out.setActivityType(ActivityType.ADMIN_ACTION);
        out.setAction(log.getAction());
        out.setTargetType("ADMIN");
        out.setTargetId(log.getAdminUserId());
        out.setMetadata(log.getMetadata());
        out.setCreatedAt(log.getCreatedAt());
        return out;
    }

    private UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setName(user.getName());
        info.setEmail(user.getEmail());
        info.setPicture(user.getPicture());
        info.setBirthdate(user.getBirthdate());
        return info;
    }
}
