package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.AdminAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminAuditLogRepository extends MongoRepository<AdminAuditLog, String> {
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
