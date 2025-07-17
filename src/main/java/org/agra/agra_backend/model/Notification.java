package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    private String userId;
    private String content;
    private NotificationType type;
    private boolean seen;
    private LocalDateTime timestamp;
}
