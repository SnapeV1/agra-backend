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

@Document(collection = "admin_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {
    @Id
    private String id;

    @Field("admin_user_id")
    private String adminUserId;

    @Field("admin_info")
    private UserInfo adminInfo;

    private String action;

    private Map<String, Object> metadata;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}
