package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.*;
import org.agra.agra_backend.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {


    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;


    private final CommentRepository commentRepository;

    private final LikeRepository likeRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;

    public static final String TARGET_TYPE_POST = "POST";
    public static final String TARGET_TYPE_COMMENT = "COMMENT";

    public PostService(PostRepository postRepository,
                       CloudinaryService cloudinaryService, CommentRepository commentRepository,
                       LikeRepository likeRepository, PostLikeRepository postLikeRepository, CommentLikeRepository commentLikeRepository) {
        this.postRepository = postRepository;
        this.cloudinaryService = cloudinaryService;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentLikeRepository = commentLikeRepository;
    }


    public List<Post> getPostsWithDetails(String currentUserId, boolean loadComments, int commentLimit) {
        List<Post> posts = postRepository.findByIsCoursePostOrderByCreatedAtDesc(false);

        if (currentUserId != null) {
            List<String> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getPostLikeStatusMap(currentUserId, postIds);

            posts.forEach(post ->
                    post.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(post.getId(), false))
            );
        }

        if (loadComments) {
            posts.forEach(post -> {
                List<Comment> comments = getCommentsForPost(post.getId(), currentUserId, commentLimit);
                post.setComments(comments);
            });
        }

        return posts;
    }


    public Page<Post> getPostsPaginated(String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findByIsCoursePostOrderByCreatedAtDesc(false, pageable);

        if (currentUserId != null) {
            List<String> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getPostLikeStatusMap(currentUserId, postIds);

            posts.getContent().forEach(post ->
                    post.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(post.getId(), false))
            );
        }

        return posts;
    }




    public List<Comment> getCommentsForPost(String postId, String currentUserId, int limit) {
        List<Comment> comments;
        if (limit > 0) {
            Pageable pageable = PageRequest.of(0, limit);
            comments = commentRepository
                    .findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId, pageable)
                    .getContent();
        } else {
            comments = commentRepository
                    .findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId);
        }

        if (currentUserId != null && !comments.isEmpty()) {
            List<String> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getLikeStatusForTargets(currentUserId, TARGET_TYPE_COMMENT, commentIds);

            comments.forEach(comment ->
                    comment.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(comment.getId(), false))
            );
        }

        comments.forEach(comment -> {
            List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
            if (currentUserId != null && !replies.isEmpty()) {
                List<String> replyIds = replies.stream().map(Comment::getId).collect(Collectors.toList());
                Map<String, Boolean> replyLikeStatus = getLikeStatusForTargets(currentUserId, TARGET_TYPE_COMMENT, replyIds);

                replies.forEach(reply ->
                        reply.setIsLikedByCurrentUser(replyLikeStatus.getOrDefault(reply.getId(), false))
                );
            }
            comment.setReplies(replies);
        });

        return comments;
    }


    @Transactional
    public Comment addComment(String postId, String userId, User userInfo, String content) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new RuntimeException("Post not found");
        }

        Comment comment = new Comment(postId, userId, userInfo, content);
        Comment savedComment = commentRepository.save(comment);

        Post post = postOpt.get();
        post.incrementCommentsCount();
        postRepository.save(post);

        return savedComment;
    }


    @Transactional
    public Comment addReply(String postId, String parentCommentId, String userId, User userInfo,
                            String content, String replyToUserId) {
        Optional<Comment> parentOpt = commentRepository.findById(parentCommentId);
        if (parentOpt.isEmpty()) {
            throw new RuntimeException("Parent comment not found");
        }

        Comment reply = new Comment(postId, userId, userInfo, content, parentCommentId, replyToUserId);
        Comment savedReply = commentRepository.save(reply);

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.incrementCommentsCount();
            postRepository.save(post);
        }

        return savedReply;
    }


    @Transactional
    public void deleteComment(String commentId, String userId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            // Clean up likes stored in both collections for backward compatibility
            commentLikeRepository.deleteByCommentId(reply.getId());
            likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, reply.getId());
        }
        commentRepository.deleteAll(replies);

        // Delete likes for the main comment as well (both collections)
        commentLikeRepository.deleteByCommentId(commentId);
        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);

        commentRepository.delete(comment);

        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(Math.max(0L, post.getCommentsCount() - (1 + replies.size())));
            postRepository.save(post);
        }
    }


    public static class ToggleLikeResult {
        public final boolean isLiked;
        public final boolean shouldNotify;
        public ToggleLikeResult(boolean isLiked, boolean shouldNotify) {
            this.isLiked = isLiked;
            this.shouldNotify = shouldNotify;
        }
    }

    @Transactional
    public ToggleLikeResult togglePostLike(String postId, String userId, User userInfo) {
        java.util.Optional<PostLike> existingOpt = postLikeRepository.findByUserIdAndPostId(userId, postId);
        boolean isLiked;
        boolean shouldNotify = false;

        if (existingOpt.isPresent()) {
            PostLike existing = existingOpt.get();
            boolean currentlyActive = (existing.getActive() == null) || Boolean.TRUE.equals(existing.getActive());
            if (currentlyActive) {
                existing.setActive(false);
                isLiked = false;
            } else {
                existing.setActive(true);
                isLiked = true;
                boolean alreadyNotified = Boolean.TRUE.equals(existing.getNotified());
                shouldNotify = !alreadyNotified;
                if (shouldNotify) {
                    existing.setNotified(true);
                }
            }
            postLikeRepository.save(existing);
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            like.setActive(true);
            // first ever like -> notify once
            like.setNotified(true);
            postLikeRepository.save(like);
            isLiked = true;
            shouldNotify = true;
        }

        java.util.Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            long actualCount = postLikeRepository.countActiveByPostId(postId);
            post.setLikesCount(actualCount);
            postRepository.save(post);
        }

        return new ToggleLikeResult(isLiked, shouldNotify);
    }


    @Transactional
    public boolean toggleCommentLike(String commentId, String userId, User userInfo) {
        boolean isLiked = toggleLike(userId, userInfo, TARGET_TYPE_COMMENT, commentId);

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            long actualCount = likeRepository.countByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);
            comment.setLikesCount(actualCount);
            commentRepository.save(comment);
        }

        return isLiked;
    }


    private boolean toggleLike(String userId, User userInfo, String targetType, String targetId) {
        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, targetType, targetId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return false;
        } else {
            Like newLike = new Like(userId, userInfo, targetType, targetId);
            likeRepository.save(newLike);
            return true;
        }
    }

    public Map<String, Boolean> getLikeStatusForTargets(String userId, String targetType, List<String> targetIds) {
        if (userId == null || targetIds.isEmpty()) {
            return targetIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        List<Like> userLikes = likeRepository.findByUserIdAndTargetTypeAndTargetIdIn(userId, targetType, targetIds);

        return targetIds.stream()
                .collect(Collectors.toMap(
                        targetId -> targetId,
                        targetId -> userLikes.stream().anyMatch(like -> like.getTargetId().equals(targetId))
                ));
    }


    @Transactional
    public void deletePost(String postId, String userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty() || !postOpt.get().getUserId().equals(userId)) {
            throw new RuntimeException("Post not found or unauthorized");
        }

        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId);
        for (Comment comment : comments) {
            deleteComment(comment.getId(), comment.getUserId());
        }

        // Remove all likes tied directly to this post
        postLikeRepository.deleteByPostId(postId);
        // Also remove any legacy/alternative like entries stored in the generic likes collection
        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_POST, postId);

        postRepository.delete(postOpt.get());
    }

    public List<Post> getAllPostsSortedByDate() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public Post createPostWithImage(String userId, User userInfo, String content, MultipartFile imageFile, boolean isCoursePost, String courseId) throws IOException {
        Post post = new Post();
        post.setUserInfo(userInfo);
        post.setUserId(userId);
        post.setContent(content);
        post.setIsCoursePost(isCoursePost);
        post.setCourseId(courseId);
        post.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        post = postRepository.save(post);

        if (imageFile != null && !imageFile.isEmpty()) {
            String sanitizedEmail = createUserFolderName(userInfo.getEmail());

            String publicId = "posts/" + sanitizedEmail + "/" + post.getId();

            Map<String, Object> uploadResult = cloudinaryService.uploadImageToFolder(imageFile, publicId);

            String imageUrl = (String) uploadResult.get("secure_url");

            post.setImageUrl(imageUrl);
            post = postRepository.save(post);
        }

        return post;
    }

    private String createUserFolderName(String email) {
        // Sanitize email for folder naming: john.doe@gmail.com -> john_doe_gmail_com
        return email.toLowerCase()
                .replace("@", "_")
                .replace(".", "_");
    }

    public java.util.Optional<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }

    private java.util.Map<String, Boolean> getPostLikeStatusMap(String userId, java.util.List<String> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return postIds == null ? java.util.Collections.emptyMap() :
                postIds.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> false));
        }
        java.util.List<PostLike> likes = postLikeRepository.findActiveByUserIdAndPostIdIn(userId, postIds);
        java.util.Set<String> likedPostIds = likes.stream()
                .map(PostLike::getPostId)
                .collect(java.util.stream.Collectors.toSet());
        return postIds.stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, likedPostIds::contains));
    }
    private java.util.Map<String, Boolean> getCommentLikeStatusMap(String userId, java.util.List<String> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return commentIds == null ? java.util.Collections.emptyMap() :
                commentIds.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> false));
        }
        java.util.List<CommentLike> likes = commentLikeRepository.findByUserIdAndCommentIdIn(userId, commentIds);
        java.util.Set<String> likedIds = likes.stream().map(CommentLike::getCommentId).collect(java.util.stream.Collectors.toSet());
        return commentIds.stream().collect(java.util.stream.Collectors.toMap(id -> id, likedIds::contains));
    }}
