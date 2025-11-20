package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.CertificateRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CertificateRecordRepository extends MongoRepository<CertificateRecord, String> {
    Optional<CertificateRecord> findByCertificateCode(String certificateCode);
    Optional<CertificateRecord> findByCourseIdAndUserId(String courseId, String userId);
    long countByRevoked(boolean revoked);
}
