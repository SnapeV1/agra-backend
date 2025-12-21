package org.agra.agra_backend.controller;

import org.agra.agra_backend.payload.ContactFormRequest;
import org.agra.agra_backend.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController controller;

    @Test
    void submitContactFormDelegatesToService() {
        ContactFormRequest request = new ContactFormRequest();
        request.setFullName("Test User");
        request.setEmail("user@example.com");
        request.setSubject("Subject");
        request.setMessage("Message");

        ResponseEntity<Void> response = controller.submitContactForm(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(contactService).sendContactMessage(request);
    }
}
