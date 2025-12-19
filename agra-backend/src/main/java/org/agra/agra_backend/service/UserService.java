package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.dao.UserRepository;

import org.agra.agra_backend.model.UserRole;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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





    @Cacheable(cacheNames = "users:dashboard", key = "'all'")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Caching(evict = {
            @CacheEvict(value = "users:dashboard", allEntries = true)
    })
    public User saveUser(User user) {
        // Ensure password is encoded if provided as plain text
        if (user.getPassword() != null && !user.getPassword().isBlank() && !isBcrypt(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // Ensure language is set (default to 'en' if missing)
        if (user.getLanguage() == null || user.getLanguage().trim().isEmpty()) {
            user.setLanguage("en");
        }
        // Ensure theme is set (default to 'light' if missing)
        if (user.getThemePreference() == null || user.getThemePreference().trim().isEmpty()) {
            user.setThemePreference("light");
        }
        if (user.getVerified() == null) {
            user.setVerified(false);
        }
        user.setRegisteredAt(new Date());
        return userRepository.save(user);
    }


    @Caching(evict = {
            @CacheEvict(value = "users:profile", key = "#user.id", condition = "#user != null && #user.id != null"),
            @CacheEvict(value = "users:dashboard", allEntries = true)
    })
    public User updateUser(User user) {
        User existingUser = findById(user.getId());

        // Log requested updates (no profile picture in this overload)
        logUserUpdateRequested(existingUser, user, false);

        // Preserve phone if not provided
        if (user.getPhone() == null) {
            user.setPhone(existingUser.getPhone());
        }

        // Preserve language if not provided
        if (user.getLanguage() == null || user.getLanguage().trim().isEmpty()) {
            user.setLanguage(existingUser.getLanguage());
        }

        // Preserve themePreference if not provided
        if (user.getThemePreference() == null || user.getThemePreference().trim().isEmpty()) {
            user.setThemePreference(existingUser.getThemePreference());
        }

        if (user.getBirthdate() == null) {
            user.setBirthdate(existingUser.getBirthdate());
        }

        // Encode password if provided raw; keep existing if not provided
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (!isBcrypt(user.getPassword()) && !user.getPassword().equals(existingUser.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        } else {
            user.setPassword(existingUser.getPassword());
        }

        // Preserve immutable fields
        if (user.getRegisteredAt() == null) {
            user.setRegisteredAt(existingUser.getRegisteredAt());
        }
        // Preserve verification status unless explicitly provided
        if (user.getVerified() == null) {
            user.setVerified(existingUser.getVerified());
        }

        return userRepository.save(user);
    }

    @Caching(evict = {
            @CacheEvict(value = "users:profile", key = "#user.id", condition = "#user != null && #user.id != null"),
            @CacheEvict(value = "users:dashboard", allEntries = true)
    })
    public User updateUser(User user, MultipartFile profilePicture) throws IOException {
        User existingUser = findById(user.getId());

        // Log requested updates (including if a profile picture is present)
        boolean profilePicProvided = profilePicture != null && !profilePicture.isEmpty();
        logUserUpdateRequested(existingUser, user, profilePicProvided);
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

        // Ensure password is encrypted like during registration
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            // Only encode if it's changed (avoid double-encoding an already hashed password)
            if (!user.getPassword().equals(existingUser.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        } else {
            // Preserve existing hashed password when no new password provided
            user.setPassword(existingUser.getPassword());
        }

        // Ensure phone is preserved if not explicitly provided
        if (user.getPhone() == null) {
            user.setPhone(existingUser.getPhone());
        }

        // Preserve language if not explicitly provided
        if (user.getLanguage() == null || user.getLanguage().trim().isEmpty()) {
            user.setLanguage(existingUser.getLanguage());
        }

        if (user.getBirthdate() == null) {
            user.setBirthdate(existingUser.getBirthdate());
        }


        user.setRegisteredAt(existingUser.getRegisteredAt());
        if (user.getVerified() == null) {
            user.setVerified(existingUser.getVerified());
        }

        return userRepository.save(user);
    }

    private void logUserUpdateRequested(User oldUser, User newUser, boolean profilePicProvided) {
        StringBuilder sb = new StringBuilder();
        sb.append("User update request for ")
                .append(oldUser.getId()).append(" /")
                .append(oldUser.getEmail() == null ? "<no-email>" : oldUser.getEmail())
                .append("\n");

        appendChange(sb, "name", oldUser.getName(), newUser.getName());
        appendChange(sb, "email", oldUser.getEmail(), newUser.getEmail());
        appendChange(sb, "phone", oldUser.getPhone(), newUser.getPhone());
        appendChange(sb, "country", oldUser.getCountry(), newUser.getCountry());
        appendChange(sb, "language", oldUser.getLanguage(), newUser.getLanguage());
        appendChange(sb, "domain", oldUser.getDomain(), newUser.getDomain());
        appendChange(sb, "role", oldUser.getRole(), newUser.getRole());
        appendChange(sb, "picture", oldUser.getPicture(), newUser.getPicture());
        appendChange(sb, "birthdate", oldUser.getBirthdate(), newUser.getBirthdate());

        boolean passwordProvided = newUser.getPassword() != null && !newUser.getPassword().isBlank();
        if (passwordProvided) {
            sb.append(" - password: [updated]\n");
        }

        sb.append(" - profilePictureProvided: ").append(profilePicProvided).append("\n");
        System.out.print(sb.toString());
    }

    private void appendChange(StringBuilder sb, String field, Object oldVal, Object newVal) {
        if (newVal == null) return; // only log when client provided a value
        String oldDisplay = oldVal == null ? "<null>" : oldVal.toString();
        String newDisplay = newVal.toString();
        if ((oldVal == null && newVal != null) || (oldVal != null && !oldVal.equals(newVal))) {
            sb.append(" - ").append(field).append(": ")
              .append(oldDisplay).append(" -> ").append(newDisplay).append("\n");
        }
    }

    private boolean isBcrypt(String value) {
        if (value == null) return false;
        // Typical BCrypt hashes are 60 chars and start with $2a$, $2b$, or $2y$
        if (value.length() < 60) return false;
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    @Caching(evict = {
            @CacheEvict(value = "users:profile", key = "#id", beforeInvocation = true),
            @CacheEvict(value = "users:dashboard", allEntries = true)
    })
    public void deleteUser(Long id) {
        if (userRepository.existsById(String.valueOf(id))) {
            userRepository.deleteById(String.valueOf(id));
        } else {
            throw new RuntimeException("User with id " + id + " not found");
        }
    }
    @Cacheable(cacheNames = "users:profile", key = "#id")
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






    /**
     * Change the authenticated user's password.
     * Validates the current password before updating to the new encoded password.
     */
    @Caching(evict = {
            @CacheEvict(value = "users:profile", key = "#userId"),
            @CacheEvict(value = "users:dashboard", allEntries = true)
    })
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must not be empty");
        }

        User user = findById(userId);

        if (user.getPassword() == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

}
