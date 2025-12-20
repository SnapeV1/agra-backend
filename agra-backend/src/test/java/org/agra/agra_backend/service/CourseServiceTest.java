package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final String DEFAULT_IMAGE =
            "https://res.cloudinary.com/dmumvupow/image/upload/v1759008723/Default_Can_you_name_the_type_of_farming_Rinjhasfamily_is_enga_2_ciduil.webp";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private CourseProgressService courseProgressService;

    @InjectMocks
    private CourseService service;

    @Test
    void createCourseUsesDefaultImageWhenNoUpload() throws IOException {
        Course course = new Course();

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("course-1");
            }
            return saved;
        });

        Course created = service.createCourse(course, null);

        assertThat(created.getImageUrl()).isEqualTo(DEFAULT_IMAGE);
        assertThat(created.getThumbnailUrl()).isEqualTo(DEFAULT_IMAGE);
        assertThat(created.getDetailImageUrl()).isEqualTo(DEFAULT_IMAGE);
    }

    @Test
    void updateCourseAppliesDefaultImageWhenMissing() throws IOException {
        Course existing = new Course();
        existing.setId("course-1");

        Course update = new Course();

        when(courseRepository.findById("course-1")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Course> result = service.updateCourse("course-1", update, null);

        assertThat(result).isPresent();
        assertThat(result.get().getImageUrl()).isEqualTo(DEFAULT_IMAGE);
    }

    @Test
    void deleteCourseUnenrollsUsersAndDeletesCourse() {
        CourseProgress progress1 = new CourseProgress();
        progress1.setUserId("user-1");
        CourseProgress progress2 = new CourseProgress();
        progress2.setUserId("user-2");

        when(courseProgressService.getCourseEnrollments("course-1"))
                .thenReturn(List.of(progress1, progress2));

        service.deleteCourse("course-1");

        verify(courseProgressService).unenrollUser("user-1", "course-1");
        verify(courseProgressService).unenrollUser("user-2", "course-1");
        verify(courseRepository).deleteById("course-1");
    }
}
