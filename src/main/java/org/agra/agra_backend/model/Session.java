package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Document(collection = "sessions")
public class Session {
    @Id
    private String id;

    private String courseId;
    private String title;
    private String description;
    private String liveStreamUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<FileRef> materials;
    private boolean chatEnabled;
    private String recordingUrl;
}
