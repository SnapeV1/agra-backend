package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.ActivityLogRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.ActivityLog;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
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
