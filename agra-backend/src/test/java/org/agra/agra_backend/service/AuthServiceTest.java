package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.LoginResponse;
import org.agra.agra_backend.payload.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService service;

    @Test
    void registerUserThrowsWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUserAppliesDefaultsAndSaves() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(" Test@Example.com ");
        request.setName("Tester");
        request.setPassword("plain");
        request.setBirthdate(new Date());
        request.setLanguage(" ");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.registerUser(request);

        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getLanguage()).isEqualTo("en");
        assertThat(saved.getThemePreference()).isEqualTo("light");
        assertThat(saved.getVerified()).isFalse();
        assertThat(saved.getPicture()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void registerUserContinuesWhenCloudinaryFolderFails() throws IOException {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("User+Test@example.com");
        request.setName("Tester");
        request.setPassword("plain");

        when(userRepository.existsByEmail("user+test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IOException("down")).when(cloudinaryService).createUserFolder("users/user_test_example_com");

        User saved = service.registerUser(request);

        assertThat(saved.getEmail()).isEqualTo("user+test@example.com");
        verify(cloudinaryService).createUserFolder("users/user_test_example_com");
    }

    @Test
    void registerUserContinuesWhenVerificationEmailFails() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setName("Tester");
        request.setPassword("plain");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("mail down")).when(emailVerificationService).sendVerificationEmail(any(User.class));

        User saved = service.registerUser(request);

        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        verify(emailVerificationService).sendVerificationEmail(saved);
    }

    @Test
    void loginThrowsWhenPasswordMismatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setPassword("hashed");

        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginReturnsTokenAndRefresh() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("pass");

        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setPassword("hashed");

        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken("user-1")).thenReturn("refresh-token");

        LoginResponse response = service.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser()).isSameAs(user);
    }
}
