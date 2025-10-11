package org.agra.agra_backend.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateSessionDto {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private Instant startTime;
    @NotNull private Instant endTime;
    private Boolean lobbyEnabled = true;
    private Boolean recordingEnabled = false;
}
