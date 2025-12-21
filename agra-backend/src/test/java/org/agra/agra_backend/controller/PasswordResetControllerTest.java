package org.agra.agra_backend.controller;

import jakarta.mail.MessagingException;
import org.agra.agra_backend.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock
    private PasswordResetService resetService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PasswordResetController controller;

    @Test
    void forgotPasswordReturnsOk() throws MessagingException {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("sent");

        ResponseEntity<?> response = controller.forgotPassword(Map.of("email", "user@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resetService).initiateReset("user@example.com");
    }

    @Test
    void testTwilioReturnsOk() {
        when(resetService.testTwilioConnection()).thenReturn("VerifyService");

        ResponseEntity<?> response = controller.testTwilio();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testTwilioReturnsBadRequestOnError() {
        doThrow(new RuntimeException("fail")).when(resetService).testTwilioConnection();

        ResponseEntity<?> response = controller.testTwilio();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void forgotPasswordSmsReturnsOk() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("sent");

        ResponseEntity<?> response = controller.forgotPasswordSms(Map.of("phone", "+1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resetService).sendResetCodeSms("+1");
    }

    @Test
    void resetPasswordReturnsOk() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("ok");

        ResponseEntity<?> response = controller.resetPassword(Map.of("token", "t", "password", "p"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resetService).resetPassword("t", "p");
    }

    @Test
    void resetPasswordReturnsBadRequestOnFailure() {
        doThrow(new RuntimeException("fail")).when(resetService).resetPassword("t", "p");

        ResponseEntity<?> response = controller.resetPassword(Map.of("token", "t", "password", "p"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPasswordSmsReturnsOk() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("ok");
        when(resetService.createResetTokenWithSmsCode("+1", "1234")).thenReturn("token");

        ResponseEntity<?> response = controller.resetPasswordSms(Map.of("phone", "+1", "code", "1234"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void resetPasswordSmsReturnsBadRequestOnFailure() {
        doThrow(new RuntimeException("fail")).when(resetService).createResetTokenWithSmsCode("+1", "1234");

        ResponseEntity<?> response = controller.resetPasswordSms(Map.of("phone", "+1", "code", "1234"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setPasswordAliasReturnsOk() {
        when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("ok");

        ResponseEntity<?> response = controller.setPassword(Map.of("token", "t", "password", "p"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resetService).resetPassword("t", "p");
    }

    @Test
    void setPasswordAliasReturnsBadRequestOnFailure() {
        doThrow(new RuntimeException("fail")).when(resetService).resetPassword("t", "p");

        ResponseEntity<?> response = controller.setPassword(Map.of("token", "t", "password", "p"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
