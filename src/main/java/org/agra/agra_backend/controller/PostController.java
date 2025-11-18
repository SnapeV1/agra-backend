package org.agra.agra_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.model.*;
import org.agra.agra_backend.service.PostService;
import org.agra.agra_backend.service.NotificationService;
import org.agra.agra_backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
PostController(PostService postService, UserService userService, SimpMessagingTemplate messagingTemplate, NotificationRepository notificationRepository, NotificationService notificationService) {
    this.postService=postService;
    this.userService = userService;
    this.notificationRepository=notificationRepository;
    this.notificationService = notificationService;
    this.messagingTemplate=messagingTemplate;
}

    @PostMapping(value = "/CreatePost", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> createPost(
            @RequestPart("post") String postJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        try {
            User userInfo = (User) authentication.getPrincipal();
            String userId = userInfo.getId();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(postJson, Map.class);

            String content = (String) payload.get("content");
            Boolean isCoursePost = (Boolean) payload.getOrDefault("isCoursePost", false);
            String courseId = (String) payload.get("courseId");

            Post createdPost = postService.createPostWithImage(
                    userId, userInfo, content, imageFile, isCoursePost, courseId
            );
            Notification notification = new Notification(
                    UUID.randomUUID().toString(),
                    "New post published: " + createdPost.getContent(),
                    NotificationType.POST,
                    LocalDateTime.now()
            );
            notificationRepository.save(notification);
            // create unseen status for all users
            notificationService.createStatusesForAllUsers(notification);

            messagingTemplate.convertAndSend("/topic/notifications", notification);


            return ResponseEntity.ok(createdPost);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Post>> getPostsWithDetails(
            @RequestParam(required = false) String currentUserId,
            @RequestParam(defaultValue = "false") boolean loadComments,
            @RequestParam(defaultValue = "3") int commentLimit,
            Authentication authentication
    ) {
        String effectiveUserId = resolveUserId(currentUserId, authentication);
        List<Post> posts = postService.getPostsWithDetails(effectiveUserId, loadComments, commentLimit);
        return ResponseEntity.ok(posts);
    }


    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable String postId,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User principal = (User) authentication.getPrincipal();
        postService.deletePost(postId, principal.getId());
        return ResponseEntity.ok("Post deleted successfully!");
    }

//comments

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable String postId,
            @RequestParam(required = false) String currentUserId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        String effectiveUserId = resolveUserId(currentUserId, authentication);
        List<Comment> comments = postService.getCommentsForPost(postId, effectiveUserId, limit);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable String postId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User principal = (User) authentication.getPrincipal();
        User userInfo = new User();
        userInfo.setId(principal.getId());
        userInfo.setName(principal.getName() != null ? principal.getName() : body.get("username"));

        String content = body.get("content");
        Comment comment = postService.addComment(postId, principal.getId(), userInfo, content);
        // Notify post owner about new comment (except when commenter is the owner)
        java.util.Optional<Post> postOpt = postService.getPostById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            String ownerId = post.getUserId();
            if (ownerId != null && !ownerId.equals(principal.getId())) {
                Notification notification = new Notification(
                        java.util.UUID.randomUUID().toString(),
                        (userInfo != null && userInfo.getName() != null ? userInfo.getName() : "Someone") + " commented on your post",
                        NotificationType.POST,
                        java.time.LocalDateTime.now()
                );
                notificationRepository.save(notification);
                notificationService.createStatusForUser(ownerId, notification);
                messagingTemplate.convertAndSendToUser(ownerId, "/queue/notifications", notification);
            }
        }
        return ResponseEntity.ok(comment);
    }


    @PostMapping("/{postId}/comments/{commentId}/reply")
    public ResponseEntity<Comment> addReply(
            @PathVariable String postId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User principal = (User) authentication.getPrincipal();
        User userInfo = new User();
        userInfo.setId(principal.getId());
        userInfo.setName(principal.getName() != null ? principal.getName() : body.get("username"));

        String content = body.get("content");
        String replyToUserId = body.get("replyToUserId");

        Comment reply = postService.addReply(postId, commentId, principal.getId(), userInfo, content, replyToUserId);
        // Notify post owner about new reply (except when replier is the owner)
        java.util.Optional<Post> postOpt = postService.getPostById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            String ownerId = post.getUserId();
            if (ownerId != null && !ownerId.equals(principal.getId())) {
                Notification notification = new Notification(
                        java.util.UUID.randomUUID().toString(),
                        (userInfo != null && userInfo.getName() != null ? userInfo.getName() : "Someone") + " commented on your post",
                        NotificationType.POST,
                        java.time.LocalDateTime.now()
                );
                notificationRepository.save(notification);
                notificationService.createStatusForUser(ownerId, notification);
                messagingTemplate.convertAndSendToUser(ownerId, "/queue/notifications", notification);
            }
        }
        return ResponseEntity.ok(reply);
    }


    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable String commentId,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User principal = (User) authentication.getPrincipal();
        postService.deleteComment(commentId, principal.getId());
        return ResponseEntity.ok("Comment deleted successfully!");
    }


    @PostMapping("/{postId}/like")
    public ResponseEntity<String> togglePostLike(
            @PathVariable String postId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            Authentication authentication
    ) {
        User userInfo;
        String effectiveUserId;

        if (authentication != null && authentication.getPrincipal() instanceof User) {
            userInfo = (User) authentication.getPrincipal();
            effectiveUserId = userInfo.getId();
        } else {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Missing required parameter: userId");
            }
            userInfo = new User();
            userInfo.setId(userId);
            userInfo.setName(username);
            effectiveUserId = userId;
        }

        PostService.ToggleLikeResult result = postService.togglePostLike(postId, effectiveUserId, userInfo);
        if (result.isLiked) {
            java.util.Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isPresent()) {
                Post post = postOpt.get();
                String ownerId = post.getUserId();
                if (ownerId != null && !ownerId.equals(effectiveUserId) && result.shouldNotify) {
                    Notification notification = new Notification(
                            java.util.UUID.randomUUID().toString(),
                            (userInfo != null && userInfo.getName() != null ? userInfo.getName() : "Someone") + " has liked your post",
                            NotificationType.POST,
                            java.time.LocalDateTime.now()
                    );
                    notificationRepository.save(notification);
                    notificationService.createStatusForUser(ownerId, notification);
                    // Deliver per-user via user destinations: clients subscribe to /user/queue/notifications
                    messagingTemplate.convertAndSendToUser(ownerId, "/queue/notifications", notification);
                }
            }
        }
        return ResponseEntity.ok(result.isLiked ? "Post liked!" : "Post unliked!");
    }
    @GetMapping("/paginated")
    public Page<Post> getPaginatedPosts(
            @RequestParam(required = false) String currentUserId,
            Pageable pageable,
            Authentication authentication
    ) {
        String effectiveUserId = resolveUserId(currentUserId, authentication);
        return postService.getPostsPaginated(effectiveUserId, pageable);
    }


    @GetMapping("/sorted")
    public ResponseEntity<List<Post>> getAllPostsSorted(
            @RequestParam(required = false) String currentUserId,
            @RequestParam(defaultValue = "false") boolean loadComments,
            @RequestParam(defaultValue = "3") int commentLimit,
            Authentication authentication
    ) {
        String effectiveUserId = resolveUserId(currentUserId, authentication);
        List<Post> posts = postService.getPostsWithDetails(effectiveUserId, loadComments, commentLimit);
        return ResponseEntity.ok(posts);
    }

    private String resolveUserId(String candidate, Authentication authentication) {
        if (candidate != null && !candidate.trim().isEmpty()) {
            return candidate;
        }
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
