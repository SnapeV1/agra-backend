package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {
    @Id
    private String id;

    private UserInfo userInfo;

    private UserInfo adminInfo;

    private String subject;
    private String attachmentUrl;
    private TicketStatus status = TicketStatus.OPEN;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
