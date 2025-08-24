package org.agra.agra_backend.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import java.time.LocalDateTime;

@Document(collection = "likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(def = "{'user_id': 1, 'target_type': 1, 'target_id': 1}", unique = true)
public class Like {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_info")
    private User userInfo;

    @Field("target_type")
    private String targetType;

    @Field("target_id")
    private String targetId;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    public Like(String userId, User userInfo, String targetType, String targetId) {
        this.userId = userId;
        this.userInfo = userInfo;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}