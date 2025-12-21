package org.agra.agra_backend.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentLikeTest {

    @Test
    void settersAndGettersWork() {
        CommentLike like = new CommentLike();
        like.setId("like-1");
        like.setUserId("user-1");
        like.setCommentId("comment-1");
        like.setActive(true);

        assertThat(like.getId()).isEqualTo("like-1");
        assertThat(like.getUserId()).isEqualTo("user-1");
        assertThat(like.getCommentId()).isEqualTo("comment-1");
        assertThat(like.getActive()).isTrue();
    }
}
