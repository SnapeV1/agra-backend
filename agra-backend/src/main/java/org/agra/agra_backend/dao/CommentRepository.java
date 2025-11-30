package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(String postId, Pageable pageable);

    List<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(String postId);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);

    long countByPostIdAndParentCommentIdIsNull(String postId);

    long countByPostId(String postId);

    List<Comment> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteByPostId(String postId);

    List<Comment> findByIdIn(List<String> commentIds);

    @Query("{'postId': {'$in': ?0}, 'parentCommentId': null}")
    List<Comment> findRecentCommentsForPosts(List<String> postIds, Pageable pageable);
}