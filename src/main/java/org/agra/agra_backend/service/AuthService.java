package org.agra.agra_backend.service;

import org.agra.agra_backend.Misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.LoginResponse;
import org.agra.agra_backend.payload.RegisterRequest;
import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Service
public class AuthService implements IAuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public AuthService(JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder, CloudinaryService cloudinaryService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }

    public User registerUser(RegisterRequest request) {
        // Normalize email to lowercase for case-insensitive handling
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(normalizedEmail); // Store normalized email
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCountry(request.getCountry());
        // Ensure language is set; default to 'en' when not provided
        String reqLang = request.getLanguage();
        user.setLanguage((reqLang == null || reqLang.trim().isEmpty()) ? "en" : reqLang.trim());
        user.setDomain(request.getDomain());
        // Set role with default fallback
        String userRole = request.getRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "FARMER"; 
        }
        user.setRole(userRole);
        // Default theme preference if not provided elsewhere
        if (user.getThemePreference() == null || user.getThemePreference().isBlank()) {
            user.setThemePreference("light");
        }
        user.setRegisteredAt(new Date());
        user.setPicture("https://res.cloudinary.com/dmumvupow/image/upload/v1756311755/defaultPicture_bqiivg.jpg");
        User savedUser = userRepository.save(user);

        try {
            String folderName = createUserFolderName(normalizedEmail);
            cloudinaryService.createUserFolder(folderName);
            System.out.println("Created Cloudinary folder for user: " + folderName);
        } catch (Exception e) {
            System.err.println("Warning: Failed to create Cloudinary folder for user " + normalizedEmail + ": " + e.getMessage());
        }

        return savedUser;
    }

    public LoginResponse login(LoginRequest request) {
        // Normalize email to lowercase for case-insensitive handling
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        User user = userRepository.findByEmail(normalizedEmail);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);
        LoginResponse response = new LoginResponse(token, user);
        System.out.println("Logged in user: " + user.getEmail() + ", profileCompleted=" + response.isProfileCompleted());
        return response;
    }

    private String createUserFolderName(String email) {
        return "users/" + email.toLowerCase()
                .replace("@", "_")
                .replace(".", "_");
    }
}
