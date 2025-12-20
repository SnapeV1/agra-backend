package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.PasswordResetTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.PasswordResetToken;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled("Disabled in CI")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService service;

    @Test
    void issueResetTokenThrowsWhenUserIdBlank() {
        assertThatThrownBy(() -> service.issueResetTokenForUserId("  "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User id is required");

        verifyNoInteractions(userRepository, tokenRepository);
    }

    @Test
    void issueResetTokenThrowsWhenUserMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueResetTokenForUserId("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(tokenRepository, never()).deleteByUserId(anyString());
    }

    @Test
    void issueResetTokenGeneratesAndPersistsHashedToken() {
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        String token = service.issueResetTokenForUserId("user-1");

        assertThat(token).isNotBlank();
        verify(tokenRepository).deleteByUserId("user-1");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getTokenHash()).isEqualTo(hash(token));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpirationDate()).isAfter(saved.getCreatedAt());
    }

    @Test
    void resetPasswordThrowsWhenTokenMissing() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("raw-token", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void resetPasswordDeletesExpiredToken() {
        PasswordResetToken token = buildToken("user-1", "raw-token",
                daysFromNow(-1));
        when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token expired");

        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPasswordDeletesTokenWhenUserMissing() {
        PasswordResetToken token = buildToken("user-2", "raw-token",
                daysFromNow(1));
        when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));
        when(userRepository.findById("user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("raw-token", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found for token");

        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPasswordUpdatesUserPasswordAndDeletesToken() {
        PasswordResetToken token = buildToken("user-3", "raw-token",
                daysFromNow(1));
        when(tokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));

        User user = new User();
        user.setId("user-3");
        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-pass");

        service.resetPassword("raw-token", "newPass");

        verify(passwordEncoder).encode("newPass");
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo("encoded-pass");
        verify(tokenRepository).delete(token);
    }

    private static PasswordResetToken buildToken(String userId, String rawToken, Date expiration) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(new Date());
        token.setExpirationDate(expiration);
        return token;
    }

    private static Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token for test", e);
        }
    }
}
