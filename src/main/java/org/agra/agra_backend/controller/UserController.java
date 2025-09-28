package org.agra.agra_backend.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;

    }

    @PostMapping("/addUser")
    public ResponseEntity<User> addUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping("AllUsers")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }



    @PutMapping(value = "/updateUser", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateUserProfile(
            @RequestPart("user") String userJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            Authentication authentication) {

        try {

            User userInfo = (User) authentication.getPrincipal();
            User existingUser = userService.findById(userInfo.getId());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            User incomingData = mapper.readValue(userJson, User.class);

            if (incomingData.getName() != null) {
                existingUser.setName(incomingData.getName());
            }
            if (incomingData.getEmail() != null) {
                // Normalize email to lowercase for consistency
                String normalizedEmail = incomingData.getEmail().toLowerCase().trim();
                existingUser.setEmail(normalizedEmail);
            }
            if (incomingData.getPhone() != null) {
                existingUser.setPhone(incomingData.getPhone());
            }
            if (incomingData.getPassword() != null) {
                existingUser.setPassword(incomingData.getPassword());
            }
            if (incomingData.getCountry() != null) {
                existingUser.setCountry(incomingData.getCountry());
            }
            if (incomingData.getLanguage() != null) {
                existingUser.setLanguage(incomingData.getLanguage());
            }
            if (incomingData.getDomain() != null) {
                existingUser.setDomain(incomingData.getDomain());
            }
            if (incomingData.getRole() != null) {
                existingUser.setRole(incomingData.getRole());
            }
            if (incomingData.getPicture() != null) {
                existingUser.setPicture(incomingData.getPicture());
            }
            if (incomingData.getRegisteredAt() != null) {
                existingUser.setRegisteredAt(incomingData.getRegisteredAt());
            }
            if (incomingData.getProgress() != null) {
                existingUser.setProgress(incomingData.getProgress());
            }


            User updatedUser = userService.updateUser(existingUser, profilePicture);
            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing user data");
        }
    }
}
