package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "post_likes")
@CompoundIndex(def = "{'userId': 1, 'postId': 1}", unique = true)
public class PostLike {
    @Id
    private String id;
    private String userId;
    private String postId;

    @CreatedDate
    private LocalDateTime createdAt;

    // Soft-like flag: if null, treat as active (backward compatible)
    private Boolean active;

    // Whether a notification has already been sent for this user-post pair
    private Boolean notified;
}
