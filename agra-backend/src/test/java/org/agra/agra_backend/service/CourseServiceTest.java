package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseFile;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.CourseTranslation;
import org.agra.agra_backend.model.QuizAnswer;
import org.agra.agra_backend.model.QuizQuestion;
import org.agra.agra_backend.model.TextContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void createCoursePopulatesNestedTranslations() throws IOException {
        Course course = new Course();
        course.setDefaultLanguage("en");

        TextContent content = new TextContent();
        content.setTitle(Map.of("en", "Lesson"));
        content.setContent(Map.of("en", "Body"));

        QuizQuestion question = new QuizQuestion();
        question.setQuestion(Map.of("en", "Question?"));
        QuizAnswer answer = new QuizAnswer();
        answer.setText(Map.of("en", "Answer"));
        question.setAnswers(List.of(answer));
        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("course-1");
            }
            return saved;
        });

        service.createCourse(course, null);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            TextContent savedContent = saved.getTextContent().get(0);
            assertThat(savedContent.getTitle()).containsKey("en");
            assertThat(savedContent.getTitle()).containsEntry("en", "Lesson");
            QuizQuestion savedQuestion = savedContent.getQuizQuestions().get(0);
            assertThat(savedQuestion.getQuestion()).containsKey("en");
            assertThat(savedQuestion.getQuestion()).containsEntry("en", "Question?");
            QuizAnswer savedAnswer = savedQuestion.getAnswers().get(0);
            assertThat(savedAnswer.getText()).containsKey("en");
            assertThat(savedAnswer.getText()).containsEntry("en", "Answer");
        });
    }

    @Test
    void localizeCourseTranslatesNestedContent() {
        Course course = new Course();
        course.setDefaultLanguage("en");
        CourseTranslation courseFr = new CourseTranslation();
        courseFr.setTitle("Titre");
        course.setTranslations(Map.of("fr", courseFr));

        TextContent content = new TextContent();
        content.setTitle(Map.of("fr", "Lecon"));
        content.setContent(Map.of("fr", "Corps"));

        QuizQuestion question = new QuizQuestion();
        question.setQuestion(Map.of("fr", "Question FR"));

        QuizAnswer answer = new QuizAnswer();
        answer.setText(Map.of("fr", "Reponse"));
        question.setAnswers(List.of(answer));

        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        Course localized = service.localizeCourse(course, Locale.FRENCH);

        assertThat(localized.getTitle()).isEqualTo("Titre");
        TextContent localizedContent = localized.getTextContent().get(0);
        assertThat(localizedContent.getTitle()).containsEntry("fr", "Lecon");
        assertThat(localizedContent.getContent()).containsEntry("fr", "Corps");
        QuizQuestion localizedQuestion = localizedContent.getQuizQuestions().get(0);
        assertThat(localizedQuestion.getQuestion()).containsEntry("fr", "Question FR");
        QuizAnswer localizedAnswer = localizedQuestion.getAnswers().get(0);
        assertThat(localizedAnswer.getText()).containsEntry("fr", "Reponse");
    }

    @Test
    void localizeCourseFallsBackToEnglishForLessons() {
        Course course = new Course();

        TextContent content = new TextContent();
        content.setTitle(Map.of("en", "Lesson"));
        content.setContent(Map.of("en", "Body"));

        QuizQuestion question = new QuizQuestion();
        question.setQuestion(Map.of("en", "Question?"));

        QuizAnswer answer = new QuizAnswer();
        answer.setText(Map.of("en", "Answer"));
        question.setAnswers(List.of(answer));

        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        Course localized = service.localizeCourse(course, Locale.GERMAN);

        TextContent localizedContent = localized.getTextContent().get(0);
        assertThat(localizedContent.getTitle()).containsEntry("en", "Lesson");
        assertThat(localizedContent.getContent()).containsEntry("en", "Body");
        QuizQuestion localizedQuestion = localizedContent.getQuizQuestions().get(0);
        assertThat(localizedQuestion.getQuestion()).containsEntry("en", "Question?");
        QuizAnswer localizedAnswer = localizedQuestion.getAnswers().get(0);
        assertThat(localizedAnswer.getText()).containsEntry("en", "Answer");
    }

    @Test
    void createCourseNormalizesNullTranslations() throws IOException {
        Course course = new Course();

        TextContent content = new TextContent();
        QuizQuestion question = new QuizQuestion();
        question.setQuestion(null);
        question.setAnswers(null);
        content.setQuizQuestions(List.of(question));
        content.setTitle(null);
        content.setContent(null);
        course.setTextContent(List.of(content));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("course-1");
            }
            return saved;
        });

        service.createCourse(course, null);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            TextContent savedContent = saved.getTextContent().get(0);
            assertThat(savedContent.getTitle()).isEmpty();
            assertThat(savedContent.getContent()).isEmpty();
            QuizQuestion savedQuestion = savedContent.getQuizQuestions().get(0);
            assertThat(savedQuestion.getQuestion()).isEmpty();
        });
    }

    @Test
    void updateCourseRethrowsImageUploadWithContext() throws IOException {
        Course existing = new Course();
        existing.setId("course-1");
        Course update = new Course();

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cloudinaryService.uploadImageToFolder(eq(file), anyString()))
                .thenThrow(new IOException("upload failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.updateCourse("course-1", update, file))
                .isInstanceOf(CourseImageUploadException.class)
                .hasMessageContaining("courseId=course-1");
    }

    @Test
    void updateCourseMergesFilesAndAssignsIds() throws IOException {
        Course existing = new Course();
        existing.setId("course-1");
        CourseFile file1 = new CourseFile();
        file1.setId("f1");
        file1.setPublicId("p1");
        CourseFile file2 = new CourseFile();
        file2.setId("f2");
        file2.setPublicId("p2");
        existing.setFiles(List.of(file1, file2));

        Course update = new Course();
        CourseFile updatedFile1 = new CourseFile();
        updatedFile1.setId("f1");
        updatedFile1.setPublicId("p1");
        updatedFile1.setName("new-name");
        CourseFile duplicatePublic = new CourseFile();
        duplicatePublic.setPublicId("p2");
        CourseFile newFile = new CourseFile();
        newFile.setPublicId("p3");
        update.setFiles(List.of(updatedFile1, duplicatePublic, newFile));

        when(courseRepository.findById("course-1")).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course result = service.updateCourse("course-1", update, null).orElseThrow();

        assertThat(result.getFiles()).hasSize(3);
        assertThat(result.getFiles())
                .anySatisfy(file -> assertThat(file.getId()).isEqualTo("f1"))
                .anySatisfy(file -> assertThat(file.getId()).isEqualTo("f2"))
                .anySatisfy(file -> {
                    assertThat(file.getPublicId()).isEqualTo("p3");
                    assertThat(file.getId()).isNotBlank();
                });
    }

    @Test
    void createCourseCreatesDefaultTranslationFromFields() throws IOException {
        Course course = new Course();
        course.setTitle("Title");
        course.setDescription("Desc");
        course.setGoals(List.of("g1"));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("course-1");
            }
            return saved;
        });

        service.createCourse(course, null);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            assertThat(saved.getDefaultLanguage()).isEqualTo("en");
            assertThat(saved.getTranslations()).containsKey("en");
            CourseTranslation translation = saved.getTranslations().get("en");
            assertThat(translation.getTitle()).isEqualTo("Title");
            assertThat(translation.getDescription()).isEqualTo("Desc");
            assertThat(translation.getGoals()).containsExactly("g1");
        });
    }

    @Test
    void createCourseGeneratesIdsForNestedTextContent() throws IOException {
        Course course = new Course();
        TextContent content = new TextContent();
        QuizQuestion question = new QuizQuestion();
        QuizAnswer answer = new QuizAnswer();
        question.setAnswers(List.of(answer));
        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("course-1");
            }
            return saved;
        });

        service.createCourse(course, null);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> {
            TextContent savedContent = saved.getTextContent().get(0);
            assertThat(savedContent.getId()).isNotBlank();
            QuizQuestion savedQuestion = savedContent.getQuizQuestions().get(0);
            assertThat(savedQuestion.getId()).isNotBlank();
            QuizAnswer savedAnswer = savedQuestion.getAnswers().get(0);
            assertThat(savedAnswer.getId()).isNotBlank();
        });
    }

    @Test
    void localizeCourseFallsBackToFirstTranslationWhenNoMatch() {
        Course course = new Course();
        course.setDefaultLanguage("fr");
        CourseTranslation translation = new CourseTranslation();
        translation.setTitle("Titulo");
        course.setTranslations(Map.of("es", translation));

        Course localized = service.localizeCourse(course, Locale.GERMAN);

        assertThat(localized.getTitle()).isEqualTo("Titulo");
    }

    @Test
    void localizeCourseUsesDefaultLanguageWhenLocaleNull() {
        Course course = new Course();
        course.setDefaultLanguage("ar");
        CourseTranslation translation = new CourseTranslation();
        translation.setTitle("Arabic");
        course.setTranslations(Map.of("ar", translation));

        Course localized = service.localizeCourse(course, null);

        assertThat(localized.getTitle()).isEqualTo("Arabic");
    }

    @Test
    void localizeCoursesReturnsEmptyWhenNoCourses() {
        List<Course> result = service.localizeCourses(List.of(), Locale.ENGLISH);

        assertThat(result).isEmpty();
    }
}
