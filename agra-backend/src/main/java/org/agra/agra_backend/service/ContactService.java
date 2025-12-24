package org.agra.agra_backend.service;

import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.payload.ContactFormRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.to-email:noreply.yeffa@gmail.com}")
    private String contactRecipient;

    @Value("${app.contact.from-email:no-reply@agra.com}")
    private String fromAddress;

    public void sendContactMessage(ContactFormRequest request) {
        System.out.println("Sending contact message"+request);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(contactRecipient);
        message.setFrom(fromAddress);
        message.setReplyTo(request.getEmail());
        message.setSubject("[Contact] " + request.getSubject());
        message.setText(buildBody(request));
        mailSender.send(message);
    }

    private String buildBody(ContactFormRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Full Name: ").append(request.getFullName()).append("\n");
        sb.append("Email: ").append(request.getEmail()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(request.getMessage());
        return sb.toString();
    }
}
