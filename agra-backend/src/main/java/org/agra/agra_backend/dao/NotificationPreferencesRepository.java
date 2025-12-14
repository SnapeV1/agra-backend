package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.NotificationPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationPreferencesRepository extends MongoRepository<NotificationPreferences, String> {
    Optional<NotificationPreferences> findByUserId(String userId);
}
