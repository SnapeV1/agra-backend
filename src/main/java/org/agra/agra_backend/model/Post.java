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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("user_info")
    private User userInfo;

    private String content;

    @Field("image_url")
    private String imageUrl;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Course-related fields
    @Field("is_course_post")
    private Boolean isCoursePost = false;

    @Field("course_id")
    private String courseId;

    @Field("comment_ids")
    private List<String> commentIds = new ArrayList<>();

    @Field("comments_count")
    private Long commentsCount = 0L;

    @Field("likes_count")
    private Long likesCount = 0L;

    @Transient
    private List<Comment> comments;

    @Transient
    private Boolean isLikedByCurrentUser;

    public void incrementCommentsCount() {
        this.commentsCount = (this.commentsCount == null ? 0L : this.commentsCount) + 1;
    }

    public void decrementCommentsCount() {
        this.commentsCount = Math.max(0L, (this.commentsCount == null ? 0L : this.commentsCount) - 1);
    }
}