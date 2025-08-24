package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CommentRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.dao.PostRepository;
import org.agra.agra_backend.model.Like;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {


    private final PostRepository postRepository;

    private final CommentRepository commentRepository;

    private final LikeRepository likeRepository;

    public static final String TARGET_TYPE_POST = "POST";
    public static final String TARGET_TYPE_COMMENT = "COMMENT";
    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }



    public List<Post> getPostsWithDetails(String currentUserId, boolean loadComments, int commentLimit) {
        List<Post> posts = postRepository.findByIsCoursePostOrderByCreatedAtDesc(false);

        if (currentUserId != null) {
            List<String> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getLikeStatusForTargets(currentUserId, TARGET_TYPE_POST, postIds);

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
            Map<String, Boolean> likeStatusMap = getLikeStatusForTargets(currentUserId, TARGET_TYPE_POST, postIds);

            posts.getContent().forEach(post ->
                    post.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(post.getId(), false))
            );
        }

        return posts;
    }


    public Post createPost(String userId, User userInfo, String content, String imageUrl,
                           Boolean isCoursePost, String courseId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setUserInfo(userInfo);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setIsCoursePost(isCoursePost != null ? isCoursePost : false);
        post.setCourseId(courseId);
        post.setLikesCount(0L);
        post.setCommentsCount(0L);

        return postRepository.save(post);
    }


    public List<Comment> getCommentsForPost(String postId, String currentUserId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Comment> comments = commentRepository
                .findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId);

        if (limit > 0 && comments.size() > limit) {
            comments = comments.subList(0, limit);
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
            likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, reply.getId());
        }
        commentRepository.deleteAll(replies);

        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);

        commentRepository.delete(comment);

        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(Math.max(0L, post.getCommentsCount() - (1 + replies.size())));
            postRepository.save(post);
        }
    }


    @Transactional
    public boolean togglePostLike(String postId, String userId, User userInfo) {
        boolean isLiked = toggleLike(userId, userInfo, TARGET_TYPE_POST, postId);

        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            long actualCount = likeRepository.countByTargetTypeAndTargetId(TARGET_TYPE_POST, postId);
            post.setLikesCount(actualCount);
            postRepository.save(post);
        }

        return isLiked;
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

        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_POST, postId);

        postRepository.delete(postOpt.get());
    }
}