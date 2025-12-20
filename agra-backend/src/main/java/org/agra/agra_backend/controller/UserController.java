package org.agra.agra_backend.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.ChangePasswordRequest;
import org.agra.agra_backend.service.CourseLikeService;
import org.agra.agra_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final CourseLikeService courseLikeService;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(CourseLikeService courseLikeService, UserService userService) {
        this.courseLikeService = courseLikeService;
        this.userService = userService;
    }

    // ------------------------------
    // BASIC USER ENDPOINTS
    // ------------------------------

    @GetMapping("/me/liked-courses")
    public ResponseEntity<?> getMyLikedCourses(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        List<String> likedIds = courseLikeService.listLikedCourseIds(user.getId());
        return ResponseEntity.ok(likedIds);
    }

    @PostMapping("/addUser")
    public ResponseEntity<User> addUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("/AllUsers")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // ------------------------------
    // USER SELF-UPDATE ENDPOINTS
    // ------------------------------

    @PutMapping(value = "/updateUser", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateUserProfile(
            @RequestPart("user") String userJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            Authentication authentication,
            HttpServletRequest request) {

        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication required");
            }

            User currentUser = (User) authentication.getPrincipal();
            User existingUser = userService.findById(currentUser.getId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            User incomingData = mapper.readValue(userJson, User.class);

            mergeUserFields(existingUser, incomingData);

            User updatedUser = userService.updateUser(existingUser, profilePicture);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user profile");
        }
    }

    @PutMapping(value = "/updateUser", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateUserProfileJson(
            @RequestBody User incomingData,
            Authentication authentication) {

        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User currentUser = (User) authentication.getPrincipal();
            User existingUser = userService.findById(currentUser.getId());

            mergeUserFields(existingUser, incomingData);

            User updatedUser = userService.updateUser(existingUser);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("Error updating JSON user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user profile");
        }
    }

    // ------------------------------
    // ADMIN ENDPOINTS
    // ------------------------------

    /**
     * Allows admins to update any user by ID.
     * This avoids overwriting the authenticated admin account.
     */
    @PutMapping(value = "/updateUser/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserByAdmin(
            @PathVariable String id,
            @RequestPart("user") String userJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            User incomingData = mapper.readValue(userJson, User.class);

            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            mergeUserFields(existingUser, incomingData);

            User updatedUser = userService.updateUser(existingUser, profilePicture);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("Error updating user by admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating user by admin"));
        }
    }

    // ------------------------------
    // PASSWORD CHANGE
    // ------------------------------

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        User user = (User) authentication.getPrincipal();
        userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // ------------------------------
    // HELPER
    // ------------------------------

    private void mergeUserFields(User existing, User incoming) {
        if (incoming.getName() != null) existing.setName(incoming.getName());
        if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
        if (incoming.getPhone() != null) existing.setPhone(incoming.getPhone());
        if (incoming.getPassword() != null) existing.setPassword(incoming.getPassword());
        if (incoming.getCountry() != null) existing.setCountry(incoming.getCountry());
        if (incoming.getLanguage() != null) existing.setLanguage(incoming.getLanguage());
        if (incoming.getDomain() != null) existing.setDomain(incoming.getDomain());
        if (incoming.getRole() != null) existing.setRole(incoming.getRole());
        if (incoming.getThemePreference() != null) existing.setThemePreference(incoming.getThemePreference());
        if (incoming.getPicture() != null) existing.setPicture(incoming.getPicture());
        if (incoming.getBirthdate() != null) existing.setBirthdate(incoming.getBirthdate());
        if (incoming.getRegisteredAt() != null) existing.setRegisteredAt(incoming.getRegisteredAt());
        if (incoming.getIsArchived() != null && !Objects.equals(incoming.getIsArchived(), existing.getIsArchived())) {
            existing.setIsArchived(incoming.getIsArchived());
        }
    }
}
