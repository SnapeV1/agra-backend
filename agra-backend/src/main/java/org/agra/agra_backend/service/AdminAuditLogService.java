package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.AdminAuditLogRepository;
import org.agra.agra_backend.model.AdminAuditLog;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Scheduled(fixedDelayString = "${admin.audit.cleanup-interval-ms:86400000}")
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        adminAuditLogRepository.deleteByCreatedAtBefore(cutoff);
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
