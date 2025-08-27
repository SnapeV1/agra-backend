package org.agra.agra_backend.service;

import org.agra.agra_backend.Misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.LoginResponse;
import org.agra.agra_backend.payload.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCountry(request.getCountry());
        user.setLanguage(request.getLanguage());
        user.setDomain(request.getDomain());
        user.setRole(request.getRole());
        user.setRegisteredAt(new Date());
        user.setPicture("https://res.cloudinary.com/dmumvupow/image/upload/v1756311755/defaultPicture_bqiivg.jpg");
        User savedUser = userRepository.save(user);

        try {
            String folderName = createUserFolderName(savedUser.getEmail());
            cloudinaryService.createUserFolder(folderName);
            System.out.println("Created Cloudinary folder for user: " + folderName);
        } catch (Exception e) {
            System.err.println("Warning: Failed to create Cloudinary folder for user " + savedUser.getEmail() + ": " + e.getMessage());
        }

        return savedUser;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);
        System.out.println(token);
        return new LoginResponse(token, user);
    }

    // Helper method to sanitize email for folder name
    private String createUserFolderName(String email) {
        // Replace @ and . with underscores, and convert to lowercase
        return "users/" + email.toLowerCase()
                .replace("@", "_")
                .replace(".", "_");
    }
}