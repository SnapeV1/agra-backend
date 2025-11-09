package org.agra.agra_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.Misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${googleClientId}")
    private String googleClientId;

    private final JwtUtil jwtService;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public LoginResponse verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String emailRaw = payload.getEmail();
            String email = emailRaw == null ? null : emailRaw.toLowerCase().trim();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String locale = (String) payload.get("locale");
            if (locale == null || locale.isBlank()) {
                locale = "en"; // default language when Google doesn't provide locale
            }
            String hostedDomain = (String) payload.get("hd");
            // Phone is not typically present in ID token unless 'phone' scope was requested
            String phone = (String) payload.get("phone_number");
            if (phone == null) {
                phone = (String) payload.get("phoneNumber");
            }
            // Country is not part of standard ID token; set only if explicitly provided by Google
            String country = null;
            if (payload.get("country") instanceof String) {
                country = (String) payload.get("country");
            }

            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Google account email is missing");
            }

            // Find existing user (normalized email) or create a new one
            User user = userRepository.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setName(name);
                // Attempt to mirror Google picture into Cloudinary like normal signup flow
                user.setPicture(picture);
                user.setLanguage(locale);
                user.setDomain(hostedDomain);
                user.setPhone(phone);
                user.setCountry(country);
                user.setRole("FARMER");
                // Default theme to light on first creation
                user.setThemePreference("light");
                user.setRegisteredAt(new Date());
                user = userRepository.save(user);

                try {
                    // Ensure user folder exists
                    String sanitizedEmail = email.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                    cloudinaryService.createUserFolder("users/" + sanitizedEmail);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to create Cloudinary folder for user " + email + ": " + e.getMessage());
                }

                // If Google returned a picture URL, upload it into Cloudinary and persist its URL
                if (picture != null && !picture.isBlank()) {
                    try {
                        var upload = cloudinaryService.uploadProfilePictureFromUrl(picture, email);
                        String uploadedUrl = upload.get("secure_url").toString();
                        user.setPicture(uploadedUrl);
                        user = userRepository.save(user);
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to mirror Google avatar for user " + email + ": " + e.getMessage());
                    }
                }
            } else {
                // Existing user: do not overwrite any fields. Only authenticate.
                // We intentionally avoid updating name, picture, phone, country, etc.
            }

            // Generate JWT for your app
            String jwt = jwtService.generateToken(user);
            LoginResponse response = new LoginResponse(jwt, user);
            System.out.println("Logged in (Google) user: " + user.getEmail() + ", profileCompleted=" + response.isProfileCompleted());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Google verification failed", e);
        }
    }
}
