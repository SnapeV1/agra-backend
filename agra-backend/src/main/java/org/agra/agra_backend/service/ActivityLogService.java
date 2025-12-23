package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.ActivityLogRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final int retentionDays;
    private final Set<String> redactedKeys;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              UserRepository userRepository,
                              @Value("${activity.logs.retention-days:180}") int retentionDays,
                              @Value("${activity.logs.redact-metadata-keys:content,message,body,details,attachmentUrl}") String redactedKeys) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.retentionDays = retentionDays;
        this.redactedKeys = parseKeySet(redactedKeys);
    }

    public void logUserActivity(User user,
                                ActivityType activityType,
                                String action,
                                String targetType,
                                String targetId,
                                Map<String, Object> metadata) {
        String userId = user != null ? user.getId() : null;
        UserInfo userInfo = toUserInfo(user);
        saveLog(userId, userInfo, activityType, action, targetType, targetId, metadata);
    }

    public void logUserActivity(String userId,
                                ActivityType activityType,
                                String action,
                                String targetType,
                                String targetId,
                                Map<String, Object> metadata) {
        UserInfo userInfo = resolveUserInfo(userId);
        saveLog(userId, userInfo, activityType, action, targetType, targetId, metadata);
    }

    public List<ActivityLog> search(String userId,
                                    ActivityType activityType,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    Integer limit) {
        List<ActivityLog> all = activityLogRepository.findAll();
        return all.stream()
                .filter(log -> userId == null || userId.equals(log.getUserId()))
                .filter(log -> activityType == null || activityType.equals(log.getActivityType()))
                .filter(log -> start == null || (log.getCreatedAt() != null && !log.getCreatedAt().isBefore(start)))
                .filter(log -> end == null || (log.getCreatedAt() != null && !log.getCreatedAt().isAfter(end)))
                .sorted(Comparator.comparing(ActivityLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit == null || limit < 1 ? Long.MAX_VALUE : limit)
                .toList();
    }

    public List<ActivityLog> searchForAdmin(String userId,
                                            ActivityType activityType,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            Integer limit) {
        return search(userId, activityType, start, end, limit).stream()
                .map(this::sanitizeForAdmin)
                .toList();
    }

    @Scheduled(fixedDelayString = "${activity.logs.cleanup-interval-ms:86400000}")
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        activityLogRepository.deleteByCreatedAtBefore(cutoff);
    }

    private void saveLog(String userId,
                         UserInfo userInfo,
                         ActivityType activityType,
                         String action,
                         String targetType,
                         String targetId,
                         Map<String, Object> metadata) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setUserInfo(userInfo);
        log.setActivityType(activityType);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setMetadata(metadata);
        activityLogRepository.save(log);
    }

    private ActivityLog sanitizeForAdmin(ActivityLog log) {
        if (log == null) {
            return null;
        }
        ActivityLog out = new ActivityLog();
        out.setId(log.getId());
        out.setUserId(log.getUserId());
        out.setUserInfo(log.getUserInfo());
        out.setActivityType(log.getActivityType());
        out.setAction(log.getAction());
        out.setTargetType(log.getTargetType());
        out.setTargetId(log.getTargetId());
        out.setCreatedAt(log.getCreatedAt());
        out.setMetadata(sanitizeMetadata(log.getMetadata()));
        return out;
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>(metadata);
        redactedKeys.forEach(out::remove);
        return out;
    }

    private Set<String> parseKeySet(String keysCsv) {
        if (keysCsv == null || keysCsv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(keysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private UserInfo resolveUserInfo(String userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(this::toUserInfo)
                .orElse(null);
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
