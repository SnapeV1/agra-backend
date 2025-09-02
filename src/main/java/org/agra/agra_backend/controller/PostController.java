package org.agra.agra_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.Comment;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PostService;
import org.agra.agra_backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

PostController(PostService postService, UserService userService){
    this.postService=postService;
    this.userService = userService;
}

    @PostMapping(value = "/CreatePost", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> createPost(
            @RequestPart("post") String postJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        try {
            User userInfo = (User) authentication.getPrincipal();
            String userId = userInfo.getId(); // Get the actual ID

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = mapper.readValue(postJson, Map.class);

            String content = (String) payload.get("content");
            Boolean isCoursePost = (Boolean) payload.getOrDefault("isCoursePost", false);
            String courseId = (String) payload.get("courseId");

            Post createdPost = postService.createPostWithImage(
                    userId, userInfo, content, imageFile, isCoursePost, courseId
            );
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
            @RequestParam(defaultValue = "3") int commentLimit
    ) {
        List<Post> posts = postService.getPostsWithDetails(currentUserId, loadComments, commentLimit);
        return ResponseEntity.ok(posts);
    }


    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable String postId,
            @RequestParam String userId
    ) {
        postService.deletePost(postId, userId);
        return ResponseEntity.ok("Post deleted successfully!");
    }

//comments

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable String postId,
            @RequestParam String userId,
            @RequestBody Map<String, String> body
    ) {
        User userInfo = new User();
        userInfo.setId(userId);
        userInfo.setName(body.get("username"));

        String content = body.get("content");
        Comment comment = postService.addComment(postId, userId, userInfo, content);
        return ResponseEntity.ok(comment);
    }


    @PostMapping("/{postId}/comments/{commentId}/reply")
    public ResponseEntity<Comment> addReply(
            @PathVariable String postId,
            @PathVariable String commentId,
            @RequestParam String userId,
            @RequestBody Map<String, String> body
    ) {
        User userInfo = new User();
        userInfo.setId(userId);
        userInfo.setName(body.get("username"));

        String content = body.get("content");
        String replyToUserId = body.get("replyToUserId");

        Comment reply = postService.addReply(postId, commentId, userId, userInfo, content, replyToUserId);
        return ResponseEntity.ok(reply);
    }


    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable String commentId,
            @RequestParam String userId
    ) {
        postService.deleteComment(commentId, userId);
        return ResponseEntity.ok("Comment deleted successfully!");
    }


    @PostMapping("/{postId}/like")
    public ResponseEntity<String> togglePostLike(
            @PathVariable String postId,
            @RequestParam String userId,
            @RequestParam String username
    ) {
        User userInfo = new User();
        userInfo.setId(userId);
        userInfo.setName(username);

        boolean isLiked = postService.togglePostLike(postId, userId, userInfo);
        return ResponseEntity.ok(isLiked ? "Post liked!" : "Post unliked!");
    }


    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<String> toggleCommentLike(
            @PathVariable String commentId,
            @RequestParam String userId,
            @RequestParam String username
    ) {
        User userInfo = new User();
        userInfo.setId(userId);
        userInfo.setName(username);

        boolean isLiked = postService.toggleCommentLike(commentId, userId, userInfo);
        return ResponseEntity.ok(isLiked ? "Comment liked!" : "Comment unliked!");
    }
    @GetMapping("/paginated")
    public Page<Post> getPaginatedPosts(
            @RequestParam(required = false) String currentUserId,
            Pageable pageable
    ) {
        return postService.getPostsPaginated(currentUserId, pageable);
    }


    @GetMapping("/sorted")
    public ResponseEntity<List<Post>> getAllPostsSortedByDate() {
        List<Post> posts = postService.getAllPostsSortedByDate();
        return ResponseEntity.ok(posts);
    }
}
