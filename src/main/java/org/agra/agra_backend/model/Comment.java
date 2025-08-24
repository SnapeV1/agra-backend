package org.agra.agra_backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;

    @Field("post_id")
    @Indexed
    private String postId;

    @Field("user_id")
    @Indexed
    private String userId;

    @Field("user_info")
    private User userInfo;

    private String content;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("parent_comment_id")
    private String parentCommentId;

    @Field("reply_to_user_id")
    private String replyToUserId;


    @Field("likes_count")
    private Long likesCount = 0L;


    @Transient
    private Boolean isLikedByCurrentUser;

    @Transient
    private java.util.List<Comment> replies;

    public Comment(String postId, String userId, User userInfo, String content) {
        this.postId = postId;
        this.userId = userId;
        this.userInfo = userInfo;
        this.content = content;
        this.likesCount = 0L;
    }

    public Comment(String postId, String userId, User userInfo, String content,
                   String parentCommentId, String replyToUserId) {
        this(postId, userId, userInfo, content);
        this.parentCommentId = parentCommentId;
        this.replyToUserId = replyToUserId;
    }
}