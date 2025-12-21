package org.agra.agra_backend.model;

import org.agra.agra_backend.payload.UserInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    void constructorSetsDefaults() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");

        Comment comment = new Comment("post-1", "user-1", userInfo, "Hello");

        assertThat(comment.getPostId()).isEqualTo("post-1");
        assertThat(comment.getUserId()).isEqualTo("user-1");
        assertThat(comment.getContent()).isEqualTo("Hello");
        assertThat(comment.getLikesCount()).isEqualTo(0L);
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNotNull();
    }
}
