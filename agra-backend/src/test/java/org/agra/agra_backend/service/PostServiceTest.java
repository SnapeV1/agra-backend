package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CommentLikeRepository;
import org.agra.agra_backend.dao.CommentRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.dao.PostLikeRepository;
import org.agra.agra_backend.dao.PostRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Comment;
import org.agra.agra_backend.model.CommentLike;
import org.agra.agra_backend.model.Like;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.PostLike;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService service;

    @Test
    void createPostWithImageSkipsUploadWhenUserMissing() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            saved.setId("post-1");
            return saved;
        });

        Post created = service.createPostWithImage("user-1", null, "Hello", imageFile, false, null);

        assertThat(created.getId()).isEqualTo("post-1");
        verify(postRepository, times(1)).save(any(Post.class));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void updatePostSkipsUploadWhenUserEmailMissing() throws IOException {
        Post post = new Post();
        post.setId("post-1");
        post.setUserId("user-1");

        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);

        User user = new User();
        user.setId("user-1");

        Post updated = service.updatePost("post-1", "user-1", "Updated", imageFile, user);

        assertThat(updated.getId()).isEqualTo("post-1");
        verifyNoInteractions(cloudinaryService);
        verify(postRepository).save(post);
    }

    @Test
    void createPostWithImageUploadsWhenUserEmailPresent() throws IOException {
        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        User user = new User();
        user.setEmail("test@example.com");
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("post-1");
            }
            return saved;
        });
        when(cloudinaryService.uploadImageToFolder(eq(imageFile), anyString()))
                .thenReturn(Map.of("secure_url", "https://cdn.example.com/img.jpg"));

        Post created = service.createPostWithImage("user-1", user, "Hello", imageFile, false, null);

        assertThat(created.getImageUrl()).isEqualTo("https://cdn.example.com/img.jpg");
        verify(cloudinaryService).uploadImageToFolder(eq(imageFile), contains("posts/"));
        verify(postRepository, times(2)).save(any(Post.class));
    }

    @Test
    void updatePostUploadsWhenImageProvided() throws IOException {
        Post post = new Post();
        post.setId("post-1");
        post.setUserId("user-1");
        post.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        MultipartFile imageFile = mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        when(cloudinaryService.uploadImageToFolder(eq(imageFile), anyString()))
                .thenReturn(Map.of("secure_url", "https://cdn.example.com/img.jpg"));

        Post updated = service.updatePost("post-1", "user-1", "  Updated ", imageFile, user);

        assertThat(updated.getContent()).isEqualTo("Updated");
        assertThat(updated.getImageUrl()).isEqualTo("https://cdn.example.com/img.jpg");
        verify(cloudinaryService).uploadImageToFolder(eq(imageFile), contains("posts/"));
    }

    @Test
    void getPostsWithDetailsEnrichesLikesAndComments() {
        Post post = new Post();
        post.setId("post-1");
        post.setUserId("author-1");
        when(postRepository.findByIsCoursePostOrderByCreatedAtDesc(false)).thenReturn(List.of(post));

        PostLike postLike = new PostLike();
        postLike.setPostId("post-1");
        when(postLikeRepository.findActiveByUserIdAndPostIdIn("user-1", List.of("post-1")))
                .thenReturn(List.of(postLike));

        User author = new User();
        author.setId("author-1");
        author.setName("Author");
        when(userRepository.findById("author-1")).thenReturn(Optional.of(author));

        Comment top = new Comment();
        top.setId("c1");
        top.setPostId("post-1");
        top.setUserId("commenter-1");
        top.setLikesCount(null);
        top.setIsLikedByCurrentUser(null);
        when(commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc("post-1"))
                .thenReturn(List.of(top));

        CommentLike commentLike = new CommentLike();
        commentLike.setCommentId("c1");
        when(commentLikeRepository.findActiveByUserIdAndCommentIdIn("user-1", List.of("c1")))
                .thenReturn(List.of(commentLike));

        User commenter = new User();
        commenter.setId("commenter-1");
        commenter.setName("Commenter");
        when(userRepository.findById("commenter-1")).thenReturn(Optional.of(commenter));

        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc("c1")).thenReturn(List.of());

        List<Post> result = service.getPostsWithDetails("user-1", true, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsLikedByCurrentUser()).isTrue();
        assertThat(result.get(0).getUserInfo()).isNotNull();
        assertThat(result.get(0).getComments()).hasSize(1);
        assertThat(result.get(0).getComments().get(0).getIsLikedByCurrentUser()).isTrue();
    }

    @Test
    void getPostsPaginatedEnrichesLikes() {
        Post post = new Post();
        post.setId("post-1");
        post.setUserId("author-1");
        Page<Post> page = new PageImpl<>(List.of(post), PageRequest.of(0, 1), 1);
        when(postRepository.findByIsCoursePostOrderByCreatedAtDesc(eq(false), any()))
                .thenReturn(page);
        PostLike like = new PostLike();
        like.setPostId("post-1");
        when(postLikeRepository.findActiveByUserIdAndPostIdIn("user-1", List.of("post-1")))
                .thenReturn(List.of(like));

        User author = new User();
        author.setId("author-1");
        when(userRepository.findById("author-1")).thenReturn(Optional.of(author));

        Page<Post> result = service.getPostsPaginated("user-1", PageRequest.of(0, 1));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsLikedByCurrentUser()).isTrue();
    }

    @Test
    void addCommentCreatesAndIncrementsPost() {
        Post post = new Post();
        post.setId("post-1");
        post.setCommentsCount(0L);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = new User();
        user.setId("user-1");
        user.setName("User");

        Comment comment = service.addComment("post-1", "user-1", user, "Hi");

        assertThat(comment.getPostId()).isEqualTo("post-1");
        verify(postRepository).save(post);
        assertThat(post.getCommentsCount()).isEqualTo(1L);
    }

    @Test
    void addReplyCreatesAndIncrementsPost() {
        Comment parent = new Comment();
        parent.setId("c1");
        when(commentRepository.findById("c1")).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post post = new Post();
        post.setId("post-1");
        post.setCommentsCount(1L);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

        User user = new User();
        user.setId("user-1");

        Comment reply = service.addReply("post-1", "c1", "user-1", user, "Reply", "u2");

        assertThat(reply.getParentCommentId()).isEqualTo("c1");
        verify(postRepository).save(post);
        assertThat(post.getCommentsCount()).isEqualTo(2L);
    }

    @Test
    void deleteCommentRejectsUnauthorized() {
        Comment comment = new Comment();
        comment.setId("c1");
        comment.setUserId("owner-1");
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));

        try {
            service.deleteComment("c1", "user-2");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("Unauthorized");
        }
    }

    @Test
    void deleteCommentRemovesRepliesAndUpdatesPost() {
        Comment comment = new Comment();
        comment.setId("c1");
        comment.setUserId("user-1");
        comment.setPostId("post-1");
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));

        Comment reply = new Comment();
        reply.setId("r1");
        reply.setPostId("post-1");
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc("c1")).thenReturn(List.of(reply));

        Post post = new Post();
        post.setId("post-1");
        post.setCommentsCount(5L);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

        service.deleteComment("c1", "user-1");

        verify(commentLikeRepository).deleteByCommentId("r1");
        verify(likeRepository).deleteByTargetTypeAndTargetId(PostService.TARGET_TYPE_COMMENT, "r1");
        verify(commentRepository).deleteAll(List.of(reply));
        verify(commentLikeRepository).deleteByCommentId("c1");
        verify(likeRepository).deleteByTargetTypeAndTargetId(PostService.TARGET_TYPE_COMMENT, "c1");
        verify(commentRepository).delete(comment);
        assertThat(post.getCommentsCount()).isEqualTo(3L);
    }

    @Test
    void togglePostLikeDeactivatesExisting() {
        PostLike existing = new PostLike();
        existing.setUserId("user-1");
        existing.setPostId("post-1");
        existing.setActive(true);
        when(postLikeRepository.findByUserIdAndPostId("user-1", "post-1")).thenReturn(Optional.of(existing));
        when(postLikeRepository.countActiveByPostId("post-1")).thenReturn(2L);
        Post post = new Post();
        post.setId("post-1");
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

        PostService.ToggleLikeResult result = service.togglePostLike("post-1", "user-1", new User());

        assertThat(result.isLiked).isFalse();
        verify(postLikeRepository).save(existing);
        assertThat(post.getLikesCount()).isEqualTo(2L);
    }

    @Test
    void togglePostLikeReactivatesAndNotifiesWhenNeeded() {
        PostLike existing = new PostLike();
        existing.setUserId("user-1");
        existing.setPostId("post-1");
        existing.setActive(false);
        existing.setNotified(false);
        when(postLikeRepository.findByUserIdAndPostId("user-1", "post-1")).thenReturn(Optional.of(existing));
        when(postLikeRepository.countActiveByPostId("post-1")).thenReturn(1L);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(new Post()));

        PostService.ToggleLikeResult result = service.togglePostLike("post-1", "user-1", new User());

        assertThat(result.isLiked).isTrue();
        assertThat(result.shouldNotify).isTrue();
        assertThat(existing.getNotified()).isTrue();
    }

    @Test
    void togglePostLikeCreatesNew() {
        when(postLikeRepository.findByUserIdAndPostId("user-1", "post-1")).thenReturn(Optional.empty());
        when(postLikeRepository.countActiveByPostId("post-1")).thenReturn(1L);
        when(postRepository.findById("post-1")).thenReturn(Optional.of(new Post()));

        PostService.ToggleLikeResult result = service.togglePostLike("post-1", "user-1", new User());

        assertThat(result.isLiked).isTrue();
        assertThat(result.shouldNotify).isTrue();
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    void toggleCommentLikeDeactivatesExisting() {
        CommentLike existing = new CommentLike();
        existing.setUserId("user-1");
        existing.setCommentId("c1");
        existing.setActive(true);
        when(commentLikeRepository.findByUserIdAndCommentId("user-1", "c1")).thenReturn(Optional.of(existing));
        when(commentLikeRepository.countActiveByCommentId("c1")).thenReturn(1L);
        when(likeRepository.countByTargetTypeAndTargetId(PostService.TARGET_TYPE_COMMENT, "c1")).thenReturn(0L);
        Comment comment = new Comment();
        comment.setId("c1");
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));

        boolean liked = service.toggleCommentLike("c1", "user-1", new User());

        assertThat(liked).isFalse();
        assertThat(comment.getLikesCount()).isEqualTo(1L);
        verify(commentLikeRepository).save(existing);
    }

    @Test
    void toggleCommentLikeRemovesLegacy() {
        when(commentLikeRepository.findByUserIdAndCommentId("user-1", "c1")).thenReturn(Optional.empty());
        when(likeRepository.existsByUserIdAndTargetTypeAndTargetId("user-1", PostService.TARGET_TYPE_COMMENT, "c1"))
                .thenReturn(true);
        when(commentLikeRepository.countActiveByCommentId("c1")).thenReturn(0L);
        when(likeRepository.countByTargetTypeAndTargetId(PostService.TARGET_TYPE_COMMENT, "c1")).thenReturn(0L);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(new Comment()));

        boolean liked = service.toggleCommentLike("c1", "user-1", new User());

        assertThat(liked).isFalse();
        verify(likeRepository).deleteByUserIdAndTargetTypeAndTargetId("user-1", PostService.TARGET_TYPE_COMMENT, "c1");
    }

    @Test
    void toggleCommentLikeCreatesNew() {
        when(commentLikeRepository.findByUserIdAndCommentId("user-1", "c1")).thenReturn(Optional.empty());
        when(likeRepository.existsByUserIdAndTargetTypeAndTargetId("user-1", PostService.TARGET_TYPE_COMMENT, "c1"))
                .thenReturn(false);
        when(commentLikeRepository.countActiveByCommentId("c1")).thenReturn(1L);
        when(likeRepository.countByTargetTypeAndTargetId(PostService.TARGET_TYPE_COMMENT, "c1")).thenReturn(0L);
        when(commentRepository.findById("c1")).thenReturn(Optional.of(new Comment()));

        boolean liked = service.toggleCommentLike("c1", "user-1", new User());

        assertThat(liked).isTrue();
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    @Test
    void deletePostRemovesDependencies() {
        Post post = new Post();
        post.setId("post-1");
        post.setUserId("user-1");
        when(postRepository.findById("post-1")).thenReturn(Optional.of(post));

        Comment comment = new Comment();
        comment.setId("c1");
        when(commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc("post-1"))
                .thenReturn(List.of(comment));

        PostService spyService = spy(service);
        doNothing().when(spyService).deleteComment("c1", comment.getUserId());

        spyService.deletePost("post-1", "user-1");

        verify(spyService).deleteComment("c1", comment.getUserId());
        verify(postLikeRepository).deleteByPostId("post-1");
        verify(likeRepository).deleteByTargetTypeAndTargetId(PostService.TARGET_TYPE_POST, "post-1");
        verify(postRepository).delete(post);
    }

    @Test
    void getCommentsForPostUsesPagingWhenLimitPositive() {
        Comment comment = new Comment();
        comment.setId("c1");
        comment.setUserId("user-1");
        Page<Comment> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 1), 1);
        when(commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(eq("post-1"), any()))
                .thenReturn(page);
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc("c1")).thenReturn(List.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        List<Comment> result = service.getCommentsForPost("post-1", null, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getCommentsForPostReturnsEmptyWhenNoIds() {
        when(commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc("post-1"))
                .thenReturn(List.of());

        List<Comment> result = service.getCommentsForPost("post-1", null, 0);

        assertThat(result).isNotNull();
    }

    @Test
    void getPostLikeStatusMapReturnsFalseForNullUser() {
        Post post = new Post();
        post.setId("p1");
        post.setUserId("author-1");
        when(postRepository.findByIsCoursePostOrderByCreatedAtDesc(false)).thenReturn(List.of(post));
        when(userRepository.findById("author-1")).thenReturn(Optional.empty());

        List<Post> result = service.getPostsWithDetails(null, false, 0);

        assertThat(result.get(0).getIsLikedByCurrentUser()).isNull();
    }

    @Test
    void getCommentLikeStatusMapUsesLegacyLikes() {
        Comment comment = new Comment();
        comment.setId("c1");
        comment.setPostId("post-1");
        comment.setUserId("user-1");
        when(commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc("post-1"))
                .thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc("c1")).thenReturn(List.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        Like legacy = new Like();
        legacy.setTargetId("c1");
        when(likeRepository.findByUserIdAndTargetTypeAndTargetIdIn("user-1", PostService.TARGET_TYPE_COMMENT, List.of("c1")))
                .thenReturn(List.of(legacy));
        when(commentLikeRepository.findActiveByUserIdAndCommentIdIn("user-1", List.of("c1")))
                .thenReturn(List.of());

        List<Comment> result = service.getCommentsForPost("post-1", "user-1", 0);

        assertThat(result.get(0).getIsLikedByCurrentUser()).isTrue();
    }

    @Test
    void getPostLikeStatusMapHandlesNullInputs() {
        @SuppressWarnings("unchecked")
        Map<String, Boolean> emptyResult = (Map<String, Boolean>) ReflectionTestUtils.invokeMethod(
                service, "getPostLikeStatusMap", null, null);
        assertThat(emptyResult).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Boolean> nullUserResult = (Map<String, Boolean>) ReflectionTestUtils.invokeMethod(
                service, "getPostLikeStatusMap", null, List.of("p1", "p2"));
        assertThat(nullUserResult).containsEntry("p1", false).containsEntry("p2", false);
    }

    @Test
    void getCommentLikeStatusMapHandlesNullInputs() {
        @SuppressWarnings("unchecked")
        Map<String, Boolean> emptyResult = (Map<String, Boolean>) ReflectionTestUtils.invokeMethod(
                service, "getCommentLikeStatusMap", "u1", null);
        assertThat(emptyResult).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Boolean> nullUserResult = (Map<String, Boolean>) ReflectionTestUtils.invokeMethod(
                service, "getCommentLikeStatusMap", null, List.of("c1"));
        assertThat(nullUserResult).containsEntry("c1", false);
    }
}
