package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "email_verification_tokens")
public class EmailVerificationToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    @Indexed(expireAfter = "PT0S")
    private Date expirationDate;

    private Date createdAt;

    public boolean isExpired() {
        return expirationDate != null && new Date().after(expirationDate);
    }
}
