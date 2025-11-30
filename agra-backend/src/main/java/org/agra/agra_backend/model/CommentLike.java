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
@Document(collection = "comment_likes")
@CompoundIndex(def = "{'userId': 1, 'commentId': 1}", unique = true)
public class CommentLike {
    @Id
    private String id;
    private String userId;
    private String commentId;

    @CreatedDate
    private LocalDateTime createdAt;

    // Soft-toggle flag mirroring post likes. Null is treated as active for legacy documents.
    private Boolean active;
}
