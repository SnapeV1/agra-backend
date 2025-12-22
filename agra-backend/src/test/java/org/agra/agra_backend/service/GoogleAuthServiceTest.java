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

import java.util.Map;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void verifyGoogleTokenThrowsWhenEmailMissing() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(null);

        GoogleAuthService service = new TestableGoogleAuthService(
                jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService, payload);

        assertThatThrownBy(() -> service.verifyGoogleToken("token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Google account email is missing");
    }

    @Test
    void verifyGoogleTokenCreatesUserWithDefaultsAndUploadsAvatar() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("new@example.com");
        payload.setEmailVerified(true);
        payload.set("name", "New User");
        payload.set("locale", "");
        payload.set("picture", "https://google/avatar.png");
        payload.set("phone_number", "+123");
        payload.set("country", "GH");

        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(jwtUtil.generateToken(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken(org.mockito.ArgumentMatchers.anyString())).thenReturn("refresh");
        when(passwordResetService.issueResetTokenForUserId(org.mockito.ArgumentMatchers.anyString())).thenReturn("reset");
        doNothing().when(cloudinaryService).createUserFolder(org.mockito.ArgumentMatchers.anyString());
        when(cloudinaryService.uploadProfilePictureFromUrl("https://google/avatar.png", "new@example.com"))
                .thenReturn(Map.of("secure_url", "https://cdn/avatar.png"));

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
        assertThat(userCaptor.getAllValues().get(0).getLanguage()).isEqualTo("en");
        assertThat(userCaptor.getAllValues().get(0).getRole()).isEqualTo("USER");
        assertThat(userCaptor.getAllValues().get(0).getPhone()).isEqualTo("+123");
        assertThat(userCaptor.getAllValues().get(0).getCountry()).isEqualTo("GH");
        assertThat(userCaptor.getAllValues().get(userCaptor.getAllValues().size() - 1).getPicture())
                .isEqualTo("https://cdn/avatar.png");
    }

    @Test
    void verifyGoogleTokenHandlesAvatarUploadFailureForExistingUser() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("existing@example.com");
        payload.setEmailVerified(false);
        payload.set("name", "Existing User");
        payload.set("locale", "fr");
        payload.set("picture", "https://google/avatar.png");

        User existing = new User();
        existing.setId("u1");
        existing.setEmail("existing@example.com");
        existing.setPicture(null);
        existing.setPassword("pass");
        existing.setVerified(false);

        when(userRepository.findByEmail("existing@example.com")).thenReturn(existing);
        when(userRepository.save(existing)).thenReturn(existing);
        when(jwtUtil.generateToken(existing)).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken("u1")).thenReturn("refresh");
        doThrow(new RuntimeException("upload failed"))
                .when(cloudinaryService)
                .uploadProfilePictureFromUrl("https://google/avatar.png", "existing@example.com");

        GoogleAuthService service = new TestableGoogleAuthService(
                jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService, payload);

        LoginResponse response = service.verifyGoogleToken("token");

        assertThat(response.getExistingAccount()).isTrue();
        assertThat(existing.getPicture()).isEqualTo("https://google/avatar.png");
    }

    @Test
    void verifyGoogleTokenHandlesResetTokenFailure() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("new@example.com");
        payload.setEmailVerified(false);

        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(jwtUtil.generateToken(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("jwt");
        when(refreshTokenService.createRefreshToken(org.mockito.ArgumentMatchers.anyString())).thenReturn("refresh");
        doThrow(new RuntimeException("reset down"))
                .when(passwordResetService)
                .issueResetTokenForUserId(org.mockito.ArgumentMatchers.anyString());
        doNothing().when(cloudinaryService).createUserFolder(org.mockito.ArgumentMatchers.anyString());

        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("new-id");
            return saved;
        });

        GoogleAuthService service = new TestableGoogleAuthService(
                jwtUtil, userRepository, cloudinaryService, passwordResetService, refreshTokenService, payload);

        LoginResponse response = service.verifyGoogleToken("token");

        assertThat(response.getPasswordResetToken()).isNull();
    }

    @Test
    void verifyGoogleTokenUsesPhoneNumberFallbackAndHostedDomain() throws Exception {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("domain@example.com");
        payload.setEmailVerified(true);
        payload.set("name", "Domain User");
        payload.set("phoneNumber", "+987");
        payload.set("hd", "example.com");

        when(userRepository.findByEmail("domain@example.com")).thenReturn(null);
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
        User created = userCaptor.getAllValues().get(0);
        assertThat(created.getPhone()).isEqualTo("+987");
        assertThat(created.getDomain()).isEqualTo("example.com");
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
