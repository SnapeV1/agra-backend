package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends MongoRepository<PostLike, String> {
    Optional<PostLike> findByUserIdAndPostId(String userId, String postId);
    boolean existsByUserIdAndPostId(String userId, String postId);
    long countByPostId(String postId);
    List<PostLike> findByUserIdAndPostIdIn(String userId, List<String> postIds);
    void deleteByPostId(String postId);

    // Count active likes; treats missing 'active' field as active for backward compatibility
    @org.springframework.data.mongodb.repository.Query(value = "{ 'postId': ?0, $or: [ { 'active': true }, { 'active': { $exists: false } } ] }", count = true)
    long countActiveByPostId(String postId);

    // Find active likes for a user across posts (active true or field missing)
    @org.springframework.data.mongodb.repository.Query(value = "{ 'userId': ?0, 'postId': { $in: ?1 }, $or: [ { 'active': true }, { 'active': { $exists: false } } ] }")
    java.util.List<org.agra.agra_backend.model.PostLike> findActiveByUserIdAndPostIdIn(String userId, java.util.List<String> postIds);
}
