package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    @Indexed(expireAfterSeconds = 0)
    private Date expirationDate;

    private Date createdAt;

    public boolean isExpired() {
        return expirationDate != null && new Date().after(expirationDate);
    }
}

