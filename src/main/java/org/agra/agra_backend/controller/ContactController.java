package org.agra.agra_backend.controller;

import jakarta.validation.Valid;
import org.agra.agra_backend.payload.ContactFormRequest;
import org.agra.agra_backend.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<Void> submitContactForm(@Valid @RequestBody ContactFormRequest request) {
        contactService.sendContactMessage(request);
        return ResponseEntity.accepted().build();
    }
}
