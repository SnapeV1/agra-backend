package org.agra.agra_backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "meeting_rooms")
@Data
@NoArgsConstructor
public class MeetingRoom {
    @Id
    private String room; // use room name as id

    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    public MeetingRoom(String room, String createdBy) {
        this.room = room;
        this.createdBy = createdBy;
    }
}