package org.agra.agra_backend.dao;

import java.util.Optional;
import org.agra.agra_backend.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUserId(String userId);
}
