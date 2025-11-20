package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.*;
import org.agra.agra_backend.model.*;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    public static final String TARGET_TYPE_POST = "POST";
    public static final String TARGET_TYPE_COMMENT = "COMMENT";

    public PostService(PostRepository postRepository,
                       CloudinaryService cloudinaryService,
                       CommentRepository commentRepository,
                       LikeRepository likeRepository,
                       PostLikeRepository postLikeRepository,
                       CommentLikeRepository commentLikeRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.cloudinaryService = cloudinaryService;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
    }

    /* ============================================================
       ===============  FEED POST RETRIEVAL  ======================
       ============================================================ */

    @Cacheable(cacheNames = "feed:recent", key = "#currentUserId + '|list|' + #loadComments + '|' + #commentLimit")
    public List<Post> getPostsWithDetails(String currentUserId, boolean loadComments, int commentLimit) {
        List<Post> posts = postRepository.findByIsCoursePostOrderByCreatedAtDesc(false);

        // Like status enrichment
        if (currentUserId != null) {
            List<String> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getPostLikeStatusMap(currentUserId, postIds);
            posts.forEach(post ->
                    post.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(post.getId(), false))
            );
        }

        // Lightweight in-memory cache to avoid repeated user lookups
        Map<String, UserInfo> userCache = new HashMap<>();

        // Enrich post author info
        posts.forEach(post -> post.setUserInfo(buildUserSummaryCached(post.getUserId(), userCache)));

        // Optionally load comments with user info
        if (loadComments) {
            posts.forEach(post -> {
                List<Comment> comments = getCommentsForPost(post.getId(), currentUserId, commentLimit, userCache);
                post.setComments(comments);
            });
        }

        return posts;
    }

    @Cacheable(cacheNames = "feed:recent", key = "#currentUserId + '|page|' + #pageable.pageNumber + '|' + #pageable.pageSize")
    public Page<Post> getPostsPaginated(String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findByIsCoursePostOrderByCreatedAtDesc(false, pageable);

        if (currentUserId != null) {
            List<String> postIds = posts.getContent().stream().map(Post::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getPostLikeStatusMap(currentUserId, postIds);
            posts.getContent().forEach(post ->
                    post.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(post.getId(), false))
            );
        }

        Map<String, UserInfo> userCache = new HashMap<>();
        posts.getContent().forEach(post -> post.setUserInfo(buildUserSummaryCached(post.getUserId(), userCache)));
        return posts;
    }

    /* ============================================================
       ===============  COMMENT RETRIEVAL  ========================
       ============================================================ */

    public List<Comment> getCommentsForPost(String postId, String currentUserId, int limit) {
        return getCommentsForPost(postId, currentUserId, limit, new HashMap<>());
    }

    private List<Comment> getCommentsForPost(String postId, String currentUserId, int limit, Map<String, UserInfo> userCache) {
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

        comments.forEach(this::ensureCommentDefaults);

        // Enrich like status for top-level comments
        if (currentUserId != null && !comments.isEmpty()) {
            List<String> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toList());
            Map<String, Boolean> likeStatusMap = getCommentLikeStatusMap(currentUserId, commentIds);
            comments.forEach(comment ->
                    comment.setIsLikedByCurrentUser(likeStatusMap.getOrDefault(comment.getId(), false))
            );
        }

        // Enrich author info and replies
        comments.forEach(comment -> {
            comment.setUserInfo(buildUserSummaryCached(comment.getUserId(), userCache));

            List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
            replies.forEach(this::ensureCommentDefaults);
            replies.forEach(reply -> reply.setUserInfo(buildUserSummaryCached(reply.getUserId(), userCache)));

            if (currentUserId != null && !replies.isEmpty()) {
                List<String> replyIds = replies.stream().map(Comment::getId).collect(Collectors.toList());
                Map<String, Boolean> replyLikeStatus = getCommentLikeStatusMap(currentUserId, replyIds);
                replies.forEach(reply -> reply.setIsLikedByCurrentUser(replyLikeStatus.getOrDefault(reply.getId(), false)));
            }
            comment.setReplies(replies);
        });

        return comments;
    }

    /* ============================================================
       ===============  COMMENT CREATION / DELETION  ===============
       ============================================================ */

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public Comment addComment(String postId, String userId, User user, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment(postId, userId, toUserInfo(user), content);
        Comment savedComment = commentRepository.save(comment);

        post.incrementCommentsCount();
        postRepository.save(post);

        return savedComment;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public Comment addReply(String postId, String parentCommentId, String userId, User user,
                            String content, String replyToUserId) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Comment reply = new Comment(postId, userId, toUserInfo(user), content, parentCommentId, replyToUserId);
        Comment savedReply = commentRepository.save(reply);

        postRepository.findById(postId).ifPresent(post -> {
            post.incrementCommentsCount();
            postRepository.save(post);
        });

        return savedReply;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            commentLikeRepository.deleteByCommentId(reply.getId());
            likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, reply.getId());
        }
        commentRepository.deleteAll(replies);

        commentLikeRepository.deleteByCommentId(commentId);
        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);

        commentRepository.delete(comment);

        postRepository.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentsCount(Math.max(0L, post.getCommentsCount() - (1 + replies.size())));
            postRepository.save(post);
        });
    }

    /* ============================================================
       ===============  LIKE TOGGLE LOGIC  ========================
       ============================================================ */

    public static class ToggleLikeResult {
        public final boolean isLiked;
        public final boolean shouldNotify;
        public ToggleLikeResult(boolean isLiked, boolean shouldNotify) {
            this.isLiked = isLiked;
            this.shouldNotify = shouldNotify;
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public ToggleLikeResult togglePostLike(String postId, String userId, User userInfo) {
        Optional<PostLike> existingOpt = postLikeRepository.findByUserIdAndPostId(userId, postId);
        boolean isLiked;
        boolean shouldNotify = false;

        if (existingOpt.isPresent()) {
            PostLike existing = existingOpt.get();
            boolean currentlyActive = existing.getActive() == null || Boolean.TRUE.equals(existing.getActive());
            if (currentlyActive) {
                existing.setActive(false);
                isLiked = false;
            } else {
                existing.setActive(true);
                isLiked = true;
                shouldNotify = !Boolean.TRUE.equals(existing.getNotified());
                if (shouldNotify) existing.setNotified(true);
            }
            postLikeRepository.save(existing);
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            like.setActive(true);
            like.setNotified(true);
            postLikeRepository.save(like);
            isLiked = true;
            shouldNotify = true;
        }

        postRepository.findById(postId).ifPresent(post -> {
            long count = postLikeRepository.countActiveByPostId(postId);
            post.setLikesCount(count);
            postRepository.save(post);
        });

        return new ToggleLikeResult(isLiked, shouldNotify);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public boolean toggleCommentLike(String commentId, String userId, User userInfo) {
        Optional<CommentLike> existing = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        boolean isLiked;

        if (existing.isPresent()) {
            CommentLike like = existing.get();
            boolean currentlyActive = like.getActive() == null || Boolean.TRUE.equals(like.getActive());
            if (currentlyActive) {
                like.setActive(false);
                isLiked = false;
            } else {
                like.setActive(true);
                isLiked = true;
            }
            commentLikeRepository.save(like);
        } else if (likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COMMENT, commentId)) {
            // Legacy like stored in the generic collection: delete to emulate an "unlike"
            likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COMMENT, commentId);
            isLiked = false;
        } else {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setActive(true);
            commentLikeRepository.save(like);
            isLiked = true;
        }

        commentRepository.findById(commentId).ifPresent(comment -> {
            long modernLikes = commentLikeRepository.countActiveByCommentId(commentId);
            long legacyLikes = likeRepository.countByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);
            comment.setLikesCount(modernLikes + legacyLikes);
            commentRepository.save(comment);
        });

        return isLiked;
    }

    /* ============================================================
       ===============  POST CREATION / DELETION  =================
       ============================================================ */

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public void deletePost(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found or unauthorized"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this post");
        }

        List<Comment> comments = commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId);
        for (Comment comment : comments) {
            deleteComment(comment.getId(), comment.getUserId());
        }

        postLikeRepository.deleteByPostId(postId);
        likeRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_POST, postId);
        postRepository.delete(post);
    }

    public List<Post> getAllPostsSortedByDate() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @Caching(evict = {
            @CacheEvict(value = {"feed:recent", "feed:topPosts"}, allEntries = true)
    })
    public Post createPostWithImage(String userId, User user, String content,
                                    MultipartFile imageFile, boolean isCoursePost, String courseId) throws IOException {
        Post post = new Post();
        post.setUserId(userId);
        post.setUserInfo(toUserInfo(user));
        post.setContent(content);
        post.setIsCoursePost(isCoursePost);
        post.setCourseId(courseId);
        post.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        post = postRepository.save(post);

        if (imageFile != null && !imageFile.isEmpty()) {
            String sanitizedEmail = createUserFolderName(user.getEmail());
            String publicId = "posts/" + sanitizedEmail + "/" + post.getId();
            Map<String, Object> uploadResult = cloudinaryService.uploadImageToFolder(imageFile, publicId);
            post.setImageUrl((String) uploadResult.get("secure_url"));
            post = postRepository.save(post);
        }

        return post;
    }

    private String createUserFolderName(String email) {
        return email.toLowerCase().replace("@", "_").replace(".", "_");
    }

    /* ============================================================
       ===============  LIKE STATUS HELPERS  =======================
       ============================================================ */

    private Map<String, Boolean> getPostLikeStatusMap(String userId, List<String> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty())
            return postIds == null ? Collections.emptyMap()
                    : postIds.stream().collect(Collectors.toMap(id -> id, id -> false));

        List<PostLike> likes = postLikeRepository.findActiveByUserIdAndPostIdIn(userId, postIds);
        Set<String> likedIds = likes.stream().map(PostLike::getPostId).collect(Collectors.toSet());
        return postIds.stream().collect(Collectors.toMap(id -> id, likedIds::contains));
    }

    private Map<String, Boolean> getCommentLikeStatusMap(String userId, List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return commentIds == null ? Collections.emptyMap()
                    : commentIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        if (userId == null) {
            return commentIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        Set<String> likedIds = new HashSet<>();

        List<CommentLike> commentLikes = commentLikeRepository.findActiveByUserIdAndCommentIdIn(userId, commentIds);
        likedIds.addAll(commentLikes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet()));

        List<Like> legacyLikes = likeRepository.findByUserIdAndTargetTypeAndTargetIdIn(userId, TARGET_TYPE_COMMENT, commentIds);
        likedIds.addAll(legacyLikes.stream().map(Like::getTargetId).collect(Collectors.toSet()));

        return commentIds.stream().collect(Collectors.toMap(id -> id, likedIds::contains));
    }

    private void ensureCommentDefaults(Comment comment) {
        if (comment == null) return;
        if (comment.getLikesCount() == null) {
            comment.setLikesCount(0L);
        }
        if (comment.getIsLikedByCurrentUser() == null) {
            comment.setIsLikedByCurrentUser(false);
        }
    }

    /* ============================================================
       ===============  USER INFO ENRICHMENT HELPERS  ==============
       ============================================================ */

    private UserInfo buildUserSummaryCached(String userId, Map<String, UserInfo> cache) {
        if (userId == null) return null;
        return cache.computeIfAbsent(userId, id ->
                userRepository.findById(id)
                        .map(this::toUserInfo)
                        .orElse(null)
        );
    }

    private UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setName(user.getName());
        info.setEmail(user.getEmail());
        info.setPicture(user.getPicture());
        return info;
    }
    public Optional<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }

}
