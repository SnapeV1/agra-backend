package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.NotificationStatusRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.NotificationStatus;
import org.agra.agra_backend.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStatusRepository notificationStatusRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationStatusRepository notificationStatusRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationStatusRepository = notificationStatusRepository;
        this.userRepository = userRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getUnseenNotificationsForUser(String userId) {
        // Unseen are those with a status record seen=false for this user
        List<NotificationStatus> statuses = notificationStatusRepository.findByUserIdAndSeenIsFalse(userId);
        List<String> ids = statuses.stream().map(NotificationStatus::getNotificationId).toList();
        return ids.isEmpty() ? java.util.List.of() : notificationRepository.findAllById(ids);
    }

    public List<Notification> getAllForUser(String userId) {
        List<NotificationStatus> statuses = notificationStatusRepository.findByUserId(userId);
        List<String> ids = statuses.stream().map(NotificationStatus::getNotificationId).toList();
        return ids.isEmpty() ? java.util.List.of() : notificationRepository.findAllById(ids);
    }

    public void markSeen(String userId, String notificationId) {
        NotificationStatus status = notificationStatusRepository
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElseGet(NotificationStatus::new);
        status.setUserId(userId);
        status.setNotificationId(notificationId);
        status.setSeen(true);
        status.setSeenAt(LocalDateTime.now());
        notificationStatusRepository.save(status);
    }

    public void createStatusesForAllUsers(Notification notification) {
        List<User> users = userRepository.findAll();
        if (users == null || users.isEmpty()) return;
        List<NotificationStatus> statuses = users.stream().map(u -> {
            NotificationStatus s = new NotificationStatus();
            s.setUserId(u.getId());
            s.setNotificationId(notification.getId());
            s.setSeen(false);
            s.setSeenAt(null);
            return s;
        }).toList();
        notificationStatusRepository.saveAll(statuses);
    }

    public void markAllSeen(String userId) {
        List<Notification> allNotifications = notificationRepository.findAll();
        List<NotificationStatus> existing = notificationStatusRepository.findByUserId(userId);

        var now = LocalDateTime.now();

        // Map existing statuses by notificationId for quick lookup
        java.util.Map<String, NotificationStatus> existingMap = existing.stream()
                .collect(java.util.stream.Collectors.toMap(NotificationStatus::getNotificationId, s -> s, (a, b) -> a));

        List<NotificationStatus> toSave = new java.util.ArrayList<>();

        for (Notification n : allNotifications) {
            NotificationStatus status = existingMap.get(n.getId());
            if (status == null) {
                status = new NotificationStatus();
                status.setUserId(userId);
                status.setNotificationId(n.getId());
            }
            if (!status.isSeen()) {
                status.setSeen(true);
                status.setSeenAt(now);
                toSave.add(status);
            }
        }

        if (!toSave.isEmpty()) {
            notificationStatusRepository.saveAll(toSave);
        }
    }

    public void deleteAllForUser(String userId) {
        notificationStatusRepository.deleteByUserId(userId);
    }

    public void createStatusForUser(String userId, Notification notification) {
        if (userId == null || notification == null || notification.getId() == null) return;
        NotificationStatus status = new NotificationStatus();
        status.setUserId(userId);
        status.setNotificationId(notification.getId());
        status.setSeen(false);
        status.setSeenAt(null);
        notificationStatusRepository.save(status);
    }
}