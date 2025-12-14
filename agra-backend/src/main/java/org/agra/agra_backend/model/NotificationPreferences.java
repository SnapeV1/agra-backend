package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_preferences")
public class NotificationPreferences {
    @Id
    private String id;
    private String userId;
    private boolean notificationsEnabled = true;
    private List<String> channels = new ArrayList<>(List.of("email", "websocket"));
    private DigestSettings digest = new DigestSettings();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigestSettings {
        private boolean enabled = false;
        private String frequency = "daily";
    }
}
