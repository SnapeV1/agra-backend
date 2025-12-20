package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CommentLikeRepository;
import org.agra.agra_backend.dao.CommentRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.dao.PostLikeRepository;
import org.agra.agra_backend.dao.PostRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Post;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
