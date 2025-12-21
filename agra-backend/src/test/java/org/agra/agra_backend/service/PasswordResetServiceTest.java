package org.agra.agra_backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.agra.agra_backend.dao.PasswordResetTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.PasswordResetToken;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PasswordResetService service;

    @BeforeEach
    void stubMessages() {
        lenient().when(messageSource.getMessage(anyString(), any(), any()))
                .thenReturn("OK");
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void initiateResetNoopsWhenEmailMissing() throws Exception {
        service.initiateReset(null);

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(mailSender);
    }

    @Test
    void initiateResetNoopsWhenUserMissing() throws Exception {
        when(userRepository.findByEmail("user@example.com")).thenReturn(null);

        service.initiateReset("user@example.com");

        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(mailSender);
    }

    @Test
    void initiateResetSendsEmailForExistingUser() throws Exception {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost");

        service.initiateReset("user@example.com");

        verify(tokenRepository).deleteByUserId("user-1");
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(message);
    }

    @Test
    void resetPasswordRejectsMissingToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("token", "password"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setExpirationDate(new Date(System.currentTimeMillis() - 1000));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("token", "password"))
                .isInstanceOf(RuntimeException.class);

        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPasswordRejectsMissingUser() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setExpirationDate(new Date(System.currentTimeMillis() + 10000));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("token", "password"))
                .isInstanceOf(RuntimeException.class);

        verify(tokenRepository).delete(token);
    }

    @Test
    void resetPasswordUpdatesUser() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setExpirationDate(new Date(System.currentTimeMillis() + 10000));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("hash");

        service.resetPassword("token", "newpass");

        assertThat(user.getPassword()).isEqualTo("hash");
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void issueResetTokenValidatesUserId() {
        assertThatThrownBy(() -> service.issueResetTokenForUserId(" "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void issueResetTokenRejectsMissingUser() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueResetTokenForUserId("user-1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void issueResetTokenPersistsToken() {
        User user = new User();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        String token = service.issueResetTokenForUserId("user-1");

        assertThat(token).isNotBlank();
        verify(tokenRepository).deleteByUserId("user-1");
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void sendResetCodeSmsNoopsWhenBlank() {
        service.sendResetCodeSms(" ");

        verifyNoInteractions(userRepository);
    }

    @Test
    void sendResetCodeSmsNoopsWhenUserMissing() {
        when(userRepository.findByPhone("+1")).thenReturn(Optional.empty());

        service.sendResetCodeSms("+1");

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void createResetTokenWithSmsCodeRequiresInputs() {
        assertThatThrownBy(() -> service.createResetTokenWithSmsCode(null, "123"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.createResetTokenWithSmsCode("+1", " "))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createResetTokenWithSmsCodeRequiresTwilioConfig() {
        ReflectionTestUtils.setField(service, "twilioAccountSid", "");
        ReflectionTestUtils.setField(service, "twilioAuthToken", "");
        ReflectionTestUtils.setField(service, "twilioVerifyServiceSid", "");

        assertThatThrownBy(() -> service.createResetTokenWithSmsCode("+1", "1234"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testTwilioConnectionRequiresConfig() {
        ReflectionTestUtils.setField(service, "twilioAccountSid", "");
        ReflectionTestUtils.setField(service, "twilioAuthToken", "");
        ReflectionTestUtils.setField(service, "twilioVerifyServiceSid", "");

        assertThatThrownBy(service::testTwilioConnection)
                .isInstanceOf(RuntimeException.class);
    }
}
