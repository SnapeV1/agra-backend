package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.NotificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationStatusRepository extends MongoRepository<NotificationStatus, String> {
    Optional<NotificationStatus> findByUserIdAndNotificationId(String userId, String notificationId);
    List<NotificationStatus> findByUserIdAndSeenIsTrue(String userId);
    List<NotificationStatus> findByUserIdAndSeenIsFalse(String userId);
    List<NotificationStatus> findByUserId(String userId);
    void deleteByUserId(String userId);
}
