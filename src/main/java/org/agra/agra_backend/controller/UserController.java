package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CourseLikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final CourseLikeService courseLikeService;

    public UserController(CourseLikeService courseLikeService) {
        this.courseLikeService = courseLikeService;
    }

    @GetMapping("/me/liked-courses")
    public ResponseEntity<?> getMyLikedCourses(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        List<String> likedIds = courseLikeService.listLikedCourseIds(userId);
        return ResponseEntity.ok(likedIds);
    }
}
