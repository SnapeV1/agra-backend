package org.agra.agra_backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.agra.agra_backend.dao.EmailVerificationTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.EmailVerificationToken;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailVerificationService service;

    @Test
    void resendVerificationRequiresEmail() {
        assertThatThrownBy(() -> service.resendVerification(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void resendVerificationNoopsWhenUserMissing() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());

        service.resendVerification("user@example.com");

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void sendVerificationEmailSkipsMissingUser() {
        service.sendVerificationEmail(null);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void sendVerificationEmailSkipsVerifiedUser() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setVerified(true);

        service.sendVerificationEmail(user);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void sendVerificationEmailPersistsTokenAndSendsEmail() throws Exception {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setVerified(false);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost");
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendVerificationEmail(user);

        verify(tokenRepository).deleteByUserId("user-1");
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(mailSender).send(message);
    }

    @Test
    void verifyTokenRequiresToken() {
        assertThatThrownBy(() -> service.verifyToken(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void verifyTokenRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyToken("token"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void verifyTokenRejectsExpiredToken() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId("user-1");
        token.setExpirationDate(new Date(System.currentTimeMillis() - 1000));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyToken("token"))
                .isInstanceOf(RuntimeException.class);
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyTokenMarksUserVerified() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId("user-1");
        token.setExpirationDate(new Date(System.currentTimeMillis() + 10000));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        service.verifyToken("token");

        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUserId("user-1");
    }
}
