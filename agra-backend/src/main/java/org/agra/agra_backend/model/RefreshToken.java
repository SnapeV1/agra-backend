package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    @Indexed(expireAfter = "PT0S")
    private Date expiresAt;

    private Date createdAt;
    private Date rotatedAt;
    private boolean revoked = false;

    public boolean isExpired() {
        return expiresAt != null && new Date().after(expiresAt);
    }
}
