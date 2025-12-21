package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.CourseTranslation;
import org.agra.agra_backend.model.QuizAnswer;
import org.agra.agra_backend.model.QuizAnswerTranslation;
import org.agra.agra_backend.model.QuizQuestion;
import org.agra.agra_backend.model.QuizQuestionTranslation;
import org.agra.agra_backend.model.TextContent;
import org.agra.agra_backend.model.TextContentTranslation;
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
        TextContentTranslation contentEn = new TextContentTranslation();
        contentEn.setTitle("Lesson");
        contentEn.setContent("Body");
        content.setTranslations(Map.of("en", contentEn));

        QuizQuestion question = new QuizQuestion();
        QuizQuestionTranslation questionEn = new QuizQuestionTranslation();
        questionEn.setQuestion("Question?");
        question.setTranslations(Map.of("en", questionEn));
        QuizAnswer answer = new QuizAnswer();
        QuizAnswerTranslation answerEn = new QuizAnswerTranslation();
        answerEn.setText("Answer");
        answer.setTranslations(Map.of("en", answerEn));
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
            assertThat(savedContent.getTranslations()).containsKey("en");
            assertThat(savedContent.getTranslations().get("en").getTitle()).isEqualTo("Lesson");
            QuizQuestion savedQuestion = savedContent.getQuizQuestions().get(0);
            assertThat(savedQuestion.getTranslations()).containsKey("en");
            assertThat(savedQuestion.getTranslations().get("en").getQuestion()).isEqualTo("Question?");
            QuizAnswer savedAnswer = savedQuestion.getAnswers().get(0);
            assertThat(savedAnswer.getTranslations()).containsKey("en");
            assertThat(savedAnswer.getTranslations().get("en").getText()).isEqualTo("Answer");
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
        TextContentTranslation contentFr = new TextContentTranslation();
        contentFr.setTitle("Lecon");
        contentFr.setContent("Corps");
        content.setTranslations(Map.of("fr", contentFr));

        QuizQuestion question = new QuizQuestion();
        QuizQuestionTranslation questionFr = new QuizQuestionTranslation();
        questionFr.setQuestion("Question FR");
        question.setTranslations(Map.of("fr", questionFr));

        QuizAnswer answer = new QuizAnswer();
        QuizAnswerTranslation answerFr = new QuizAnswerTranslation();
        answerFr.setText("Reponse");
        answer.setTranslations(Map.of("fr", answerFr));
        question.setAnswers(List.of(answer));

        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        Course localized = service.localizeCourse(course, Locale.FRENCH);

        assertThat(localized.getTitle()).isEqualTo("Titre");
        TextContent localizedContent = localized.getTextContent().get(0);
        assertThat(localizedContent.getTranslations().get("fr").getTitle()).isEqualTo("Lecon");
        assertThat(localizedContent.getTranslations().get("fr").getContent()).isEqualTo("Corps");
        QuizQuestion localizedQuestion = localizedContent.getQuizQuestions().get(0);
        assertThat(localizedQuestion.getTranslations().get("fr").getQuestion()).isEqualTo("Question FR");
        QuizAnswer localizedAnswer = localizedQuestion.getAnswers().get(0);
        assertThat(localizedAnswer.getTranslations().get("fr").getText()).isEqualTo("Reponse");
    }

    @Test
    void localizeCourseFallsBackToEnglishForLessons() {
        Course course = new Course();

        TextContent content = new TextContent();
        TextContentTranslation contentEn = new TextContentTranslation();
        contentEn.setTitle("Lesson");
        contentEn.setContent("Body");
        content.setTranslations(Map.of("en", contentEn));

        QuizQuestion question = new QuizQuestion();
        QuizQuestionTranslation questionEn = new QuizQuestionTranslation();
        questionEn.setQuestion("Question?");
        question.setTranslations(Map.of("en", questionEn));

        QuizAnswer answer = new QuizAnswer();
        QuizAnswerTranslation answerEn = new QuizAnswerTranslation();
        answerEn.setText("Answer");
        answer.setTranslations(Map.of("en", answerEn));
        question.setAnswers(List.of(answer));

        content.setQuizQuestions(List.of(question));
        course.setTextContent(List.of(content));

        Course localized = service.localizeCourse(course, Locale.GERMAN);

        TextContent localizedContent = localized.getTextContent().get(0);
        assertThat(localizedContent.getTranslations().get("en").getTitle()).isEqualTo("Lesson");
        assertThat(localizedContent.getTranslations().get("en").getContent()).isEqualTo("Body");
        QuizQuestion localizedQuestion = localizedContent.getQuizQuestions().get(0);
        assertThat(localizedQuestion.getTranslations().get("en").getQuestion()).isEqualTo("Question?");
        QuizAnswer localizedAnswer = localizedQuestion.getAnswers().get(0);
        assertThat(localizedAnswer.getTranslations().get("en").getText()).isEqualTo("Answer");
    }
}
