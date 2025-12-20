package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveUserAppliesDefaultsAndEncodesPassword() {
        User user = new User();
        user.setPassword("plain");
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.saveUser(user);

        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getLanguage()).isEqualTo("en");
        assertThat(saved.getThemePreference()).isEqualTo("light");
        assertThat(saved.getVerified()).isFalse();
    }

    @Test
    void changePasswordThrowsWhenNewPasswordBlank() {
        assertThatThrownBy(() -> service.changePassword("user-1", "old", " "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordMismatch() {
        User user = new User();
        user.setId("user-1");
        user.setPassword("hashed");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("user-1", "old", "new"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePasswordUpdatesWhenValid() {
        User user = new User();
        user.setId("user-1");
        user.setPassword("hashed");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");

        service.changePassword("user-1", "old", "new");

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
    }

    @Test
    void getCurrentUserOrThrowReturnsPrincipalUser() {
        User user = new User();
        user.setId("user-1");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User resolved = service.getCurrentUserOrThrow();

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void getCurrentUserOrThrowFailsWhenAnonymous() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(service::getCurrentUserOrThrow)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
