package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.dao.UserRepository;

import org.agra.agra_backend.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }





    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        user.setRegisteredAt(new Date());
        return userRepository.save(user);
    }


    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(User user, MultipartFile profilePicture) throws IOException {
        User existingUser = findById(user.getId());
        System.out.println("email "+user.getEmail());
        System.out.println(user);
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                Map<String, Object> uploadResult = cloudinaryService.uploadProfilePicture(profilePicture, user.getEmail());

                String pictureUrl = uploadResult.get("secure_url").toString();
                user.setPicture(pictureUrl);

                System.out.println("Profile picture updated for user: " + user.getEmail() + " -> " + pictureUrl);

            } catch (IOException e) {
                System.err.println("Failed to upload profile picture for user: " + user.getEmail());
                throw new IOException("Failed to upload profile picture: " + e.getMessage(), e);
            }
        } else {
            user.setPicture(existingUser.getPicture());
        }


        user.setRegisteredAt(existingUser.getRegisteredAt());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (userRepository.existsById(String.valueOf(id))) {
            userRepository.deleteById(String.valueOf(id));
        } else {
            throw new RuntimeException("User with id " + id + " not found");
        }
    }
    public User findById(String id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }




    public User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = auth.getPrincipal();

        // Your JwtAuthFilter sets the DB User as principal
        if (principal instanceof User u) {
            return u;
        }

        // Fallbacks if something else set a different principal type (rare in your setup)
        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            // springUser.getUsername() could be email or id depending on your Auth setup
            // Try id first, then email (case-insensitive)
            return userRepository.findById(springUser.getUsername())
                    .or(() -> userRepository.findByEmailIgnoreCase(springUser.getUsername()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
        if (principal instanceof String name) {
            return userRepository.findById(name)
                    .or(() -> userRepository.findByEmailIgnoreCase(name))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unsupported principal type");
    }


    /** Convenience: return null instead of throwing. */
    public User getCurrentUserOrNull() {
        try { return getCurrentUserOrThrow(); }
        catch (ResponseStatusException ex) { return null; }
    }






}
