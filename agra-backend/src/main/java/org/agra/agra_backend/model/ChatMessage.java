package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    private String sessionId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
}
