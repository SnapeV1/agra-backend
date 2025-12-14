package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User,String> {
    Boolean existsByEmail(String email);
    User findByEmail(String email);
    Optional<User> findByPhone(String phone);
    
    // Case-insensitive email queries (backup methods if needed)
    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Boolean existsByEmailIgnoreCase(String email);

    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Optional<User> findByEmailIgnoreCase(String email);
}
