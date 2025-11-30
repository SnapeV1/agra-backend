package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Like;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository

public interface LikeRepository extends MongoRepository<Like, String> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);

    long countByTargetTypeAndTargetId(String targetType, String targetId);

    List<Like> findByTargetTypeAndTargetId(String targetType, String targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);

    void deleteByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);

    void deleteByTargetTypeAndTargetId(String targetType, String targetId);

    List<Like> findByUserId(String userId);

    @Query("{'userId': ?0, 'targetType': ?1, 'targetId': {'$in': ?2}}")
    List<Like> findByUserIdAndTargetTypeAndTargetIdIn(String userId, String targetType, List<String> targetIds);

    @Query("{'targetType': 'POST'}")
    List<Like> findPostLikes();
}