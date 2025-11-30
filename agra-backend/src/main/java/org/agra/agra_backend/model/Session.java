package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Document("sessions")
public class Session {
    @Id private String id;
    private String courseId;
    private String title;
    private String description;

    private String roomName;
    private String liveStreamUrl;
    private Instant startTime;
    private Instant endTime;

    private Boolean lobbyEnabled = true;
    private Boolean recordingEnabled = false;

    private String recordingUrl;
    private List<String> attendeeIds;
    private Map<String, Long> watchSecondsByUserId;

    private Instant createdAt;
    private Instant updatedAt;
}
