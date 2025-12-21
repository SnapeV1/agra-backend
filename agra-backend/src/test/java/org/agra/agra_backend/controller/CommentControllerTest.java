package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private CommentController controller;

    @Test
    void toggleCommentLikeReturnsBadRequestWhenMissingUserId() {
        ResponseEntity<String> response = controller.toggleCommentLike("comment-1", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void toggleCommentLikeUsesAuthenticatedUser() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        user.setName("User");
        when(authentication.getPrincipal()).thenReturn(user);
        when(postService.toggleCommentLike("comment-1", "user-1", user)).thenReturn(true);

        ResponseEntity<String> response = controller.toggleCommentLike("comment-1", null, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Comment liked!");
    }
}
