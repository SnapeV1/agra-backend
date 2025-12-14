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
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(JwtUtil jwtUtil,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       CloudinaryService cloudinaryService,
                       EmailVerificationService emailVerificationService,
                       RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
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
        // Enforce default role for signups to prevent privilege escalation
        user.setRole("USER");
        user.setVerified(false);
        // Default theme preference if not provided elsewhere
        if (user.getThemePreference() == null || user.getThemePreference().isBlank()) {
            user.setThemePreference("light");
        }
        user.setRegisteredAt(new Date());
        if (request.getPicture() != null && !request.getPicture().trim().isEmpty()) {
            user.setPicture(request.getPicture().trim());
        } else {
            user.setPicture("https://res.cloudinary.com/dmumvupow/image/upload/v1756311755/defaultPicture_bqiivg.jpg");
        }
        User savedUser = userRepository.save(user);

        try {
            String folderName = createUserFolderName(normalizedEmail);
            cloudinaryService.createUserFolder(folderName);
        } catch (Exception e) {
            System.err.println("Warning: Failed to create Cloudinary folder for user " + normalizedEmail + ": " + e.getMessage());
        }

        try {
            emailVerificationService.sendVerificationEmail(savedUser);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send verification email to " + normalizedEmail + ": " + e.getMessage());
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
        String refresh = refreshTokenService.createRefreshToken(user.getId());
        LoginResponse response = new LoginResponse(token, user, true, null, refresh);
        return response;
    }

    private String createUserFolderName(String email) {
        // Use the same sanitization strategy as CloudinaryService profile uploads
        String sanitizedEmail = email.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return "users/" + sanitizedEmail;
    }
}
