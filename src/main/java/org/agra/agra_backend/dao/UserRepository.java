package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User,String> {
    Boolean existsByEmail(String email);

}
