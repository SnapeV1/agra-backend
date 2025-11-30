package org.agra.agra_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket_messages")
public class TicketMessage {
    @Id
    private String id;

    @Indexed
    private String ticketId;

    @JsonIgnore
    private String senderId;
    @JsonIgnore
    private String recipientId;

    private UserInfo sender;
    private UserInfo recipient;

    private String content;
    private String attachmentUrl;
    private LocalDateTime timestamp;
    private boolean isAdminMessage;
}
