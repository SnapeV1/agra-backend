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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private CommentController controller;

    @Test
    void toggleCommentLikeReturnsBadRequestWhenNoUserId() {
        ResponseEntity<String> response = controller.toggleCommentLike("c1", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void toggleCommentLikeReturnsBadRequestWhenPrincipalNotUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not-a-user");

        ResponseEntity<String> response = controller.toggleCommentLike("c1", null, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void toggleCommentLikeUsesAuthenticatedUser() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(postService.toggleCommentLike(eq("c1"), eq("user-1"), any(User.class))).thenReturn(true);

        ResponseEntity<String> response = controller.toggleCommentLike("c1", null, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Comment liked!");
    }

    @Test
    void toggleCommentLikeUsesUserIdParam() {
        when(postService.toggleCommentLike(eq("c1"), eq("user-2"), any(User.class))).thenReturn(false);

        ResponseEntity<String> response = controller.toggleCommentLike("c1", "user-2", "Name", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Comment unliked!");
        verify(postService).toggleCommentLike(eq("c1"), eq("user-2"), any(User.class));
    }
}
