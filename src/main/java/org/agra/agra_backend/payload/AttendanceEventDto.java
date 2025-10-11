package org.agra.agra_backend.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceEventDto {
    @NotNull
    private EventType type;
    private Long secondsWatched;
    public enum EventType { JOIN, LEAVE, STATS }
}
