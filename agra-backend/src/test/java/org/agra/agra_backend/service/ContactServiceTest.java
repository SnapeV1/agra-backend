package org.agra.agra_backend.service;

import org.agra.agra_backend.payload.ContactFormRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ContactService service;

    @Test
    void sendContactMessageBuildsMail() {
        ReflectionTestUtils.setField(service, "contactRecipient", "support@example.com");
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@example.com");

        ContactFormRequest request = new ContactFormRequest();
        request.setFullName("Test User");
        request.setEmail("user@example.com");
        request.setSubject("Help");
        request.setMessage("Need assistance.");

        service.sendContactMessage(request);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("support@example.com");
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getReplyTo()).isEqualTo("user@example.com");
        assertThat(message.getSubject()).contains("Help");
        assertThat(message.getText()).contains("Test User");
    }
}
