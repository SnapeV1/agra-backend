package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.AdminSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingsRepository extends MongoRepository<AdminSettings, String> {
}
