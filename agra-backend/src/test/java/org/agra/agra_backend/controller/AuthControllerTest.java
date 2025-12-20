package org.agra.agra_backend.controller;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.model.RefreshToken;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.LoginResponse;
import org.agra.agra_backend.payload.RegisterRequest;
import org.agra.agra_backend.service.AuthService;
import org.agra.agra_backend.service.EmailVerificationService;
import org.agra.agra_backend.service.GoogleAuthService;
import org.agra.agra_backend.service.RefreshTokenService;
import org.agra.agra_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuthService authService;
    @Mock
    private GoogleAuthService googleAuthService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController controller;

    @Test
    void registerUserReturnsOk() {
        RegisterRequest request = new RegisterRequest();

        ResponseEntity<Object> response = controller.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).registerUser(request);
    }

    @Test
    void registerUserReturnsBadRequestOnFailure() {
        RegisterRequest request = new RegisterRequest();
        doThrow(new RuntimeException("fail")).when(authService).registerUser(request);

        ResponseEntity<Object> response = controller.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loginReturnsOk() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenReturn(new LoginResponse("token", new User(), true, null, "refresh"));

        ResponseEntity<Object> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getCurrentUserRejectsMissingHeader() {
        ResponseEntity<Object> response = controller.getCurrentUser(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentUserReturnsNotFoundWhenUserMissing() {
        DefaultClaims claims = new DefaultClaims();
        claims.setSubject("user-1");
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.getCurrentUser("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWhenTokenInvalid() {
        when(jwtUtil.extractAllClaims("token")).thenThrow(new JwtException("bad"));

        ResponseEntity<Object> response = controller.getCurrentUser("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshReturnsNewTokens() {
        RefreshToken stored = new RefreshToken();
        stored.setUserId("user-1");
        when(refreshTokenService.validateRefreshToken("refresh")).thenReturn(stored);
        User user = new User();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt");
        when(refreshTokenService.rotateRefreshToken(stored)).thenReturn("new-refresh");

        ResponseEntity<Object> response = controller.refresh(Map.of("refreshToken", "refresh"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void googleLoginRejectsMissingToken() {
        ResponseEntity<Object> response = controller.googleLogin(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void logoutAlwaysReturnsOk() {
        ResponseEntity<Object> response = controller.logout(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(refreshTokenService).revokeByToken(null);
    }
}
