package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final PostService postService;

    public CommentController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping({"/api/comments/{commentId}/like", "/api/posts/comments/{commentId}/like"})
    public ResponseEntity<String> toggleCommentLike(
            @PathVariable String commentId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            Authentication authentication
    ) {
        User userInfo;
        String effectiveUserId;

        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userInfo = user;
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

        boolean isLiked = postService.toggleCommentLike(commentId, effectiveUserId, userInfo);
        return ResponseEntity.ok(isLiked ? "Comment liked!" : "Comment unliked!");
    }
}
