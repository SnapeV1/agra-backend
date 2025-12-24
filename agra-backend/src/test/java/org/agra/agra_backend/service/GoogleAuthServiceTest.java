package org.agra.agra_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthServiceTest {

    @Test
    void verifyGoogleTokenRejectsMissingEmail() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        GoogleAuthService service = buildService(payload);

        assertThatThrownBy(() -> service.verifyGoogleToken("token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void verifyGoogleTokenCreatesUserWithDefaults() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("Test@Example.com");
        payload.setEmailVerified(true);
        payload.set("name", "Test User");
        payload.set("locale", "");

        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserRepository userRepository = mock(UserRepository.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        when(userRepository.findByEmail("test@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("user-1");
            }
            return saved;
        });
        when(passwordResetService.issueResetTokenForUserId("user-1")).thenReturn("reset-1");
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-1");
        when(refreshTokenService.createRefreshToken("user-1")).thenReturn("refresh-1");

        GoogleAuthService service = new GoogleAuthService(jwtUtil, userRepository, cloudinaryService,
                passwordResetService, refreshTokenService) {
            @Override
            protected GoogleIdToken.Payload verifyAndGetPayload(String idTokenString) {
                return payload;
            }
        };

        LoginResponse response = service.verifyGoogleToken("token");

        assertThat(response.getToken()).isEqualTo("jwt-1");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-1");
        assertThat(response.getPasswordResetToken()).isEqualTo("reset-1");
        assertThat(response.getExistingAccount()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getThemePreference()).isEqualTo("light");
        assertThat(saved.getLanguage()).isEqualTo("en");
        assertThat(saved.getVerified()).isTrue();
        assertThat(saved.getRegisteredAt()).isNotNull();
        assertThat(saved.getPicture()).contains("defaultPicture");
    }

    private GoogleAuthService buildService(GoogleIdToken.Payload payload) {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserRepository userRepository = mock(UserRepository.class);
        CloudinaryService cloudinaryService = mock(CloudinaryService.class);
        PasswordResetService passwordResetService = mock(PasswordResetService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        return new GoogleAuthService(jwtUtil, userRepository, cloudinaryService,
                passwordResetService, refreshTokenService) {
            @Override
            protected GoogleIdToken.Payload verifyAndGetPayload(String idTokenString) {
                return payload;
            }
        };
    }
}
