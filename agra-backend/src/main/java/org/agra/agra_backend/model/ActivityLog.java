package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_info")
    private UserInfo userInfo;

    @Field("activity_type")
    private ActivityType activityType;

    private String action;

    @Field("target_type")
    private String targetType;

    @Field("target_id")
    private String targetId;

    private Map<String, Object> metadata;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}
