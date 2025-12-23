package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private ActivityLogService activityLogService;

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
    void saveUserSkipsEncodingForBcryptPassword() {
        User user = new User();
        user.setPassword("$2a$10$123456789012345678901234567890123456789012345678901234567890");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.saveUser(user);

        assertThat(saved.getPassword()).isEqualTo(user.getPassword());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUserPreservesFieldsAndEncodesPasswordWhenNeeded() {
        User existing = new User();
        existing.setId("user-1");
        existing.setPhone("123");
        existing.setLanguage("fr");
        existing.setThemePreference("dark");
        existing.setBirthdate(new Date());
        existing.setRegisteredAt(new Date());
        existing.setVerified(true);
        existing.setPassword("hashed");

        User update = new User();
        update.setId("user-1");
        update.setPassword("new");
        update.setPhone(null);
        update.setLanguage("");
        update.setThemePreference(null);
        update.setVerified(null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.updateUser(update);

        assertThat(saved.getPhone()).isEqualTo("123");
        assertThat(saved.getLanguage()).isEqualTo("fr");
        assertThat(saved.getThemePreference()).isEqualTo("dark");
        assertThat(saved.getBirthdate()).isEqualTo(existing.getBirthdate());
        assertThat(saved.getRegisteredAt()).isEqualTo(existing.getRegisteredAt());
        assertThat(saved.getVerified()).isTrue();
        assertThat(saved.getPassword()).isEqualTo("encoded-new");
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityLogService).logUserActivity(
                eq(saved),
                eq(ActivityType.PROFILE_UPDATE),
                eq("Updated profile"),
                eq("USER"),
                eq("user-1"),
                metadataCaptor.capture()
        );
        Object fields = metadataCaptor.getValue().get("updatedFields");
        assertThat(fields).isInstanceOf(List.class);
        assertThat(fields).asInstanceOf(list(String.class)).contains("password");
    }

    @Test
    void updateUserSkipsEncodingWhenPasswordUnchanged() {
        User existing = new User();
        existing.setId("user-1");
        existing.setPassword("hashed");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User update = new User();
        update.setId("user-1");
        update.setPassword("hashed");

        User saved = service.updateUser(update);

        assertThat(saved.getPassword()).isEqualTo("hashed");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserWithProfilePictureUploadsAndEncodes() throws IOException {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("user@example.com");
        existing.setPassword("hashed");
        existing.setPicture("old");

        User update = new User();
        update.setId("user-1");
        update.setEmail("user@example.com");
        update.setPassword("new");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(cloudinaryService.uploadProfilePicture(file, "user@example.com"))
                .thenReturn(java.util.Map.of("secure_url", "https://img/new"));
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.updateUser(update, file);

        assertThat(saved.getPicture()).isEqualTo("https://img/new");
        assertThat(saved.getPassword()).isEqualTo("encoded-new");
        ArgumentCaptor<Map<String, Object>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityLogService).logUserActivity(
                eq(saved),
                eq(ActivityType.PROFILE_UPDATE),
                eq("Updated profile"),
                eq("USER"),
                eq("user-1"),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getValue()).containsEntry("profilePictureProvided", true);
        Object fields = metadataCaptor.getValue().get("updatedFields");
        assertThat(fields).isInstanceOf(List.class);
        assertThat(fields).asInstanceOf(list(String.class)).contains("picture");
    }

    @Test
    void updateUserWithProfilePictureThrowsOnUploadFailure() throws IOException {
        User existing = new User();
        existing.setId("user-1");
        existing.setEmail("user@example.com");
        existing.setPassword("hashed");

        User update = new User();
        update.setId("user-1");
        update.setEmail("user@example.com");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(cloudinaryService.uploadProfilePicture(file, "user@example.com"))
                .thenThrow(new IOException("bad"));

        assertThatThrownBy(() -> service.updateUser(update, file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to upload profile picture");
    }

    @Test
    void deleteUserThrowsWhenMissing() {
        when(userRepository.existsById("1")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteUserDeletesWhenPresent() {
        when(userRepository.existsById("1")).thenReturn(true);

        service.deleteUser(1L);

        verify(userRepository).deleteById("1");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
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

    @Test
    void getCurrentUserOrThrowResolvesSpringUserById() {
        User user = new User();
        user.setId("user-1");
        UserDetails springUser = org.springframework.security.core.userdetails.User
                .withUsername("user-1")
                .password("x")
                .authorities(Collections.emptyList())
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(springUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User resolved = service.getCurrentUserOrThrow();

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void getCurrentUserOrThrowResolvesStringPrincipalByEmail() {
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User resolved = service.getCurrentUserOrThrow();

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void getCurrentUserOrThrowFailsForUnsupportedPrincipal() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(42, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(service::getCurrentUserOrThrow)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentUserOrNullReturnsNullOnFailure() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(service.getCurrentUserOrNull()).isNull();
    }
}
