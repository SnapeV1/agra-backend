package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.Comment;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;


    @PostMapping
    public ResponseEntity<Post> createPost(
            @RequestParam String userId,
            @RequestBody Map<String, Object> payload
    ) {
        User userInfo = new User();
        userInfo.setId(userId);
        userInfo.setName((String) payload.get("username"));

        String content = (String) payload.get("content");
        String imageUrl = (String) payload.get("imageUrl");
        Boolean isCoursePost = (Boolean) payload.getOrDefault("isCoursePost", false);
        String courseId = (String) payload.get("courseId");

        Post post = postService.createPost(userId, userInfo, content, imageUrl, isCoursePost, courseId);
        return ResponseEntity.ok(post);
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
}
