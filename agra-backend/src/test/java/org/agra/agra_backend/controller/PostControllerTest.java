package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.model.Comment;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.NotificationService;
import org.agra.agra_backend.service.PostService;
import org.agra.agra_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;
    @Mock
    private UserService userService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostController controller;

    @Test
    void createPostReturnsOk() throws Exception {
        User user = new User();
        user.setId("user-1");
        user.setName("Alice");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        Post post = new Post();
        post.setId("post-1");
        post.setContent("Hello");
        when(postService.createPostWithImage(eq("user-1"), eq(user), eq("Hello"), isNull(), eq(true), eq("course-1")))
                .thenReturn(post);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Post> response = controller.createPost(
                "{\"content\":\"Hello\",\"isCoursePost\":true,\"courseId\":\"course-1\"}",
                null,
                authentication
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(post);
        verify(notificationService).createStatusesForAllUsers(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), any(Notification.class));
    }

    @Test
    void createPostReturnsServerErrorOnFailure() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        when(postService.createPostWithImage(anyString(), any(), anyString(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("fail"));

        ResponseEntity<Post> response = controller.createPost(
                "{\"content\":\"Hello\"}",
                null,
                authentication
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getPostsWithDetailsUsesAuthenticatedUser() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(postService.getPostsWithDetails("user-1", false, 3)).thenReturn(List.of());

        ResponseEntity<List<Post>> response = controller.getPostsWithDetails(null, false, 3, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).getPostsWithDetails("user-1", false, 3);
    }

    @Test
    void getPostsWithDetailsPrefersQueryUserId() {
        when(postService.getPostsWithDetails("query-user", true, 5)).thenReturn(List.of());

        ResponseEntity<List<Post>> response = controller.getPostsWithDetails("query-user", true, 5, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).getPostsWithDetails("query-user", true, 5);
    }

    @Test
    void deletePostRequiresAuthentication() {
        ResponseEntity<String> response = controller.deletePost("post-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deletePostDeletesForOwner() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        ResponseEntity<String> response = controller.deletePost("post-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).deletePost("post-1", "user-1");
    }

    @Test
    void updatePostRequiresAuthentication() {
        ResponseEntity<Post> response = controller.updatePost("post-1", "{}", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updatePostReturnsOk() throws Exception {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        Post updated = new Post();
        updated.setId("post-1");
        when(postService.updatePost(eq("post-1"), eq("user-1"), eq("Hello"), isNull(), eq(user)))
                .thenReturn(updated);

        ResponseEntity<Post> response = controller.updatePost("post-1", "{\"content\":\"Hello\"}", null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    void updatePostReturnsBadRequestOnInvalidJson() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        ResponseEntity<Post> response = controller.updatePost("post-1", "not-json", null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCommentsReturnsOk() {
        when(postService.getCommentsForPost("post-1", null, 10)).thenReturn(List.of());

        ResponseEntity<List<Comment>> response = controller.getComments("post-1", null, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getCommentsUsesAuthenticatedUser() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(postService.getCommentsForPost("post-1", "user-1", 2)).thenReturn(List.of());

        ResponseEntity<List<Comment>> response = controller.getComments("post-1", null, 2, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).getCommentsForPost("post-1", "user-1", 2);
    }

    @Test
    void addCommentRequiresAuthentication() {
        ResponseEntity<Comment> response = controller.addComment("post-1", Map.of("content", "Hi"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void addCommentNotifiesPostOwner() {
        User principal = new User();
        principal.setId("user-1");
        principal.setName("Alice");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        Comment comment = new Comment();
        when(postService.addComment(eq("post-1"), eq("user-1"), any(User.class), eq("Hi"))).thenReturn(comment);
        Post post = new Post();
        post.setUserId("owner-1");
        when(postService.getPostById("post-1")).thenReturn(Optional.of(post));

        ResponseEntity<Comment> response = controller.addComment("post-1", Map.of("content", "Hi"), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).createStatusForUser(eq("owner-1"), any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("owner-1"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    void addCommentDoesNotNotifyWhenOwnerIsAuthor() {
        User principal = new User();
        principal.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(postService.addComment(eq("post-1"), eq("user-1"), any(User.class), eq("Hi")))
                .thenReturn(new Comment());
        Post post = new Post();
        post.setUserId("user-1");
        when(postService.getPostById("post-1")).thenReturn(Optional.of(post));

        ResponseEntity<Comment> response = controller.addComment("post-1", Map.of("content", "Hi"), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationService, never()).createStatusForUser(anyString(), any(Notification.class));
    }

    @Test
    void addReplyRequiresAuthentication() {
        ResponseEntity<Comment> response = controller.addReply("post-1", "comment-1", Map.of(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void addReplyNotifiesPostOwner() {
        User principal = new User();
        principal.setId("user-1");
        principal.setName("Alice");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        Comment comment = new Comment();
        when(postService.addReply(eq("post-1"), eq("comment-1"), eq("user-1"), any(User.class), eq("Hi"), isNull()))
                .thenReturn(comment);
        Post post = new Post();
        post.setUserId("owner-1");
        when(postService.getPostById("post-1")).thenReturn(Optional.of(post));

        ResponseEntity<Comment> response = controller.addReply(
                "post-1",
                "comment-1",
                Map.of("content", "Hi"),
                authentication
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).createStatusForUser(eq("owner-1"), any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("owner-1"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    void addReplyDoesNotNotifyWhenPostMissing() {
        User principal = new User();
        principal.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(postService.addReply(eq("post-1"), eq("comment-1"), eq("user-1"), any(User.class), eq("Hi"), isNull()))
                .thenReturn(new Comment());
        when(postService.getPostById("post-1")).thenReturn(Optional.empty());

        ResponseEntity<Comment> response = controller.addReply(
                "post-1",
                "comment-1",
                Map.of("content", "Hi"),
                authentication
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationService, never()).createStatusForUser(anyString(), any(Notification.class));
    }

    @Test
    void deleteCommentRequiresAuthentication() {
        ResponseEntity<String> response = controller.deleteComment("comment-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteCommentDeletesForOwner() {
        User user = new User();
        user.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        ResponseEntity<String> response = controller.deleteComment("comment-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).deleteComment("comment-1", "user-1");
    }

    @Test
    void togglePostLikeRequiresUserIdWhenUnauthenticated() {
        ResponseEntity<String> response = controller.togglePostLike("post-1", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void togglePostLikeSendsNotification() {
        User principal = new User();
        principal.setId("user-1");
        principal.setName("Alice");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(postService.togglePostLike("post-1", "user-1", principal))
                .thenReturn(new PostService.ToggleLikeResult(true, true));
        Post post = new Post();
        post.setUserId("owner-1");
        when(postService.getPostById("post-1")).thenReturn(Optional.of(post));

        ResponseEntity<String> response = controller.togglePostLike("post-1", null, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Post liked!");
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).createStatusForUser(eq("owner-1"), any(Notification.class));
    }

    @Test
    void togglePostLikeUsesUserIdWhenUnauthenticated() {
        when(postService.togglePostLike(eq("post-1"), eq("user-1"), any(User.class)))
                .thenReturn(new PostService.ToggleLikeResult(false, false));

        ResponseEntity<String> response = controller.togglePostLike("post-1", "user-1", "User", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Post unliked!");
    }

    @Test
    void togglePostLikeSkipsNotificationWhenNotNeeded() {
        User principal = new User();
        principal.setId("user-1");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(postService.togglePostLike("post-1", "user-1", principal))
                .thenReturn(new PostService.ToggleLikeResult(true, false));
        Post post = new Post();
        post.setUserId("owner-1");
        when(postService.getPostById("post-1")).thenReturn(Optional.of(post));

        ResponseEntity<String> response = controller.togglePostLike("post-1", null, null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getPaginatedPostsDelegatesToService() {
        Page<Post> page = new PageImpl<>(List.of(), PageRequest.of(0, 3), 0);
        when(postService.getPostsPaginated(eq("user-1"), any())).thenReturn(page);

        Page<Post> response = controller.getPaginatedPosts("user-1", PageRequest.of(0, 3), null);

        assertThat(response).isEqualTo(page);
    }

    @Test
    void getPaginatedPostsUsesAuthenticatedUserWhenMissingParam() {
        User user = new User();
        user.setId("auth-user");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        Page<Post> page = new PageImpl<>(List.of(), PageRequest.of(0, 3), 0);
        when(postService.getPostsPaginated(eq("auth-user"), any())).thenReturn(page);

        Page<Post> response = controller.getPaginatedPosts(null, PageRequest.of(0, 3), authentication);

        assertThat(response).isEqualTo(page);
    }

    @Test
    void getAllPostsSortedReturnsPosts() {
        when(postService.getPostsWithDetails("user-1", true, 5)).thenReturn(List.of());

        ResponseEntity<List<Post>> response = controller.getAllPostsSorted("user-1", true, 5, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
