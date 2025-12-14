package org.agra.agra_backend.dao;

import java.util.Optional;
import org.agra.agra_backend.model.EmailVerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationTokenRepository extends MongoRepository<EmailVerificationToken, String> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteByUserId(String userId);
}
