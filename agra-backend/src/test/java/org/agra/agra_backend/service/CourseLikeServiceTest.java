package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.Like;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseLikeService service;

    @Test
    void likeCourseReturnsFalseWhenCourseMissing() {
        when(courseRepository.findById("c1")).thenReturn(Optional.empty());

        boolean result = service.likeCourse("u1", "c1");

        assertThat(result).isFalse();
        verifyNoMoreInteractions(likeRepository);
    }

    @Test
    void likeCourseIsIdempotent() {
        when(courseRepository.findById("c1")).thenReturn(Optional.of(new Course()));
        when(likeRepository.existsByUserIdAndTargetTypeAndTargetId("u1", CourseLikeService.TARGET_TYPE_COURSE, "c1"))
                .thenReturn(true);

        boolean result = service.likeCourse("u1", "c1");

        assertThat(result).isTrue();
        verify(likeRepository).existsByUserIdAndTargetTypeAndTargetId("u1", CourseLikeService.TARGET_TYPE_COURSE, "c1");
    }

    @Test
    void likeCoursePersistsLike() {
        when(courseRepository.findById("c1")).thenReturn(Optional.of(new Course()));
        when(likeRepository.existsByUserIdAndTargetTypeAndTargetId("u1", CourseLikeService.TARGET_TYPE_COURSE, "c1"))
                .thenReturn(false);

        boolean result = service.likeCourse("u1", "c1");

        assertThat(result).isTrue();
        ArgumentCaptor<Like> captor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isEqualTo("c1");
    }

    @Test
    void unlikeCourseDeletesLike() {
        when(courseRepository.findById("c1")).thenReturn(Optional.of(new Course()));
        doNothing().when(likeRepository).deleteByUserIdAndTargetTypeAndTargetId("u1", CourseLikeService.TARGET_TYPE_COURSE, "c1");

        boolean result = service.unlikeCourse("u1", "c1");

        assertThat(result).isTrue();
        verify(likeRepository).deleteByUserIdAndTargetTypeAndTargetId("u1", CourseLikeService.TARGET_TYPE_COURSE, "c1");
    }

    @Test
    void listLikedCourseIdsFiltersByType() {
        Like courseLike = new Like();
        courseLike.setTargetType(CourseLikeService.TARGET_TYPE_COURSE);
        courseLike.setTargetId("c1");
        Like otherLike = new Like();
        otherLike.setTargetType("POST");
        when(likeRepository.findByUserId("u1")).thenReturn(List.of(courseLike, otherLike));

        List<String> result = service.listLikedCourseIds("u1");

        assertThat(result).containsExactly("c1");
    }
}
