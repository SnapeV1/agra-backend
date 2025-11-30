package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
}