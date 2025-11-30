package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "notification_status")
@CompoundIndexes({
        @CompoundIndex(name = "user_notification_idx", def = "{ 'userId': 1, 'notificationId': 1 }", unique = true)
})
public class NotificationStatus {
    @Id
    private String id;

    private String userId;
    private String notificationId;
    private boolean seen;
    private LocalDateTime seenAt;
}

