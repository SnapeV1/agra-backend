package org.agra.agra_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinResponse {
    private String roomName;
    private String domain;
    private String jwt;
    private String displayName;
    private String avatarUrl;
}
