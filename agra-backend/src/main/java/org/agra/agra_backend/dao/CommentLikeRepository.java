package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.CommentLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends MongoRepository<CommentLike, String> {
    Optional<CommentLike> findByUserIdAndCommentId(String userId, String commentId);
    boolean existsByUserIdAndCommentId(String userId, String commentId);
    long countByCommentId(String commentId);
    List<CommentLike> findByUserIdAndCommentIdIn(String userId, List<String> commentIds);
    void deleteByCommentId(String commentId);

    @Query(value = "{ 'commentId': ?0, $or: [ { 'active': true }, { 'active': { $exists: false } } ] }", count = true)
    long countActiveByCommentId(String commentId);

    @Query(value = "{ 'userId': ?0, 'commentId': { $in: ?1 }, $or: [ { 'active': true }, { 'active': { $exists: false } } ] }")
    List<CommentLike> findActiveByUserIdAndCommentIdIn(String userId, List<String> commentIds);
}
