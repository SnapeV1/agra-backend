package org.agra.agra_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void verifyGoogleTokenUpdatesExistingUser() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("User@Example.com");
        payload.setEmailVerified(true);
        payload.set("name", "Test User");
        payload.set("locale", "");

        User existing = new User();
        existing.setId("u1");
        existing.setEmail("user@example.com");
        existing.setPicture("pic");
        existing.setPassword("pass");
        existing.setVerified(false);

        when(userRepository.findByEmail("user@example.com")).thenReturn(existing);
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtUtil.generateToken(existing)).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken("u1")).thenReturn("refresh");

        GoogleAuthService service = new TestableGoogleAuthService(
                jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService, payload);

        LoginResponse response = service.verifyGoogleToken("token");

        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(response.getExistingAccount()).isTrue();
        assertThat(existing.getVerified()).isTrue();
        verify(userRepository).save(existing);
    }

    @Test
    void verifyGoogleTokenCreatesNewUser() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("new@example.com");
        payload.setEmailVerified(false);
        payload.set("name", "New User");
        payload.set("locale", "fr");

        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(jwtUtil.generateToken(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken(org.mockito.ArgumentMatchers.anyString())).thenReturn("refresh");
        when(passwordResetService.issueResetTokenForUserId(org.mockito.ArgumentMatchers.anyString())).thenReturn("reset");
        doNothing().when(cloudinaryService).createUserFolder(org.mockito.ArgumentMatchers.anyString());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            User saved = userCaptor.getValue();
            if (saved.getId() == null) {
                saved.setId("new-id");
            }
            if (saved.getRegisteredAt() == null) {
                saved.setRegisteredAt(new Date());
            }
            return saved;
        });

        GoogleAuthService service = new TestableGoogleAuthService(
                jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService, payload);

        LoginResponse response = service.verifyGoogleToken("token");

        assertThat(response.getExistingAccount()).isFalse();
        assertThat(response.getPasswordResetToken()).isEqualTo("reset");
        verify(cloudinaryService).createUserFolder(org.mockito.ArgumentMatchers.contains("users/"));
    }

    private static class TestableGoogleAuthService extends GoogleAuthService {
        private final GoogleIdToken.Payload payload;

        TestableGoogleAuthService(JwtUtil jwtUtil,
                                  UserRepository userRepository,
                                  CloudinaryService cloudinaryService,
                                  PasswordResetService passwordResetService,
                                  RefreshTokenService refreshTokenService,
                                  GoogleIdToken.Payload payload) {
            super(jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService);
            this.payload = payload;
        }

        @Override
        protected GoogleIdToken.Payload verifyAndGetPayload(String idTokenString) {
            return payload;
        }
    }
}
