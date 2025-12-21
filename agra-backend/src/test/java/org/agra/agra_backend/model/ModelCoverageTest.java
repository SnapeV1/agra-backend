package org.agra.agra_backend.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCoverageTest {

    @Test
    void passwordResetTokenReportsExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpirationDate(new Date(System.currentTimeMillis() - 1000));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void postLikeStoresFields() {
        PostLike like = new PostLike("id", "user-1", "post-1", LocalDateTime.now(), true, false);

        assertThat(like.getUserId()).isEqualTo("user-1");
        assertThat(like.getPostId()).isEqualTo("post-1");
    }

    @Test
    void postCommentCountersRespectBounds() {
        Post post = new Post();
        post.setCommentsCount(null);
        post.incrementCommentsCount();
        post.decrementCommentsCount();
        post.decrementCommentsCount();

        assertThat(post.getCommentsCount()).isZero();
    }

    @Test
    void quizAnswerDelegatingConstructorSetsText() {
        QuizAnswer answer = new QuizAnswer("Option A");

        assertThat(answer.getText()).isEqualTo("Option A");
        assertThat(answer.isCorrect()).isFalse();
    }

    @Test
    void quizQuestionAcceptsAnswers() {
        QuizQuestion question = new QuizQuestion();
        question.setAnswers(List.of(new QuizAnswer("A")));

        assertThat(question.getAnswers()).hasSize(1);
    }

    @Test
    void textContentStoresQuestions() {
        TextContent content = new TextContent();
        content.setQuizQuestions(List.of(new QuizQuestion()));

        assertThat(content.getQuizQuestions()).hasSize(1);
    }

    @Test
    void trainingKitStoresFields() {
        TrainingKit kit = new TrainingKit();
        kit.setTitle("Guide");
        kit.setCourseId("course-1");

        assertThat(kit.getTitle()).isEqualTo("Guide");
        assertThat(kit.getCourseId()).isEqualTo("course-1");
    }

    @Test
    void ticketMessageStoresFlags() {
        TicketMessage message = new TicketMessage();
        message.setAdminMessage(true);

        assertThat(message.isAdminMessage()).isTrue();
    }

    @Test
    void userRoleIncludesAdmin() {
        assertThat(UserRole.valueOf("ADMIN")).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void courseFileStoresFields() {
        CourseFile file = new CourseFile();
        file.setName("Doc");
        file.setType("pdf");
        file.setUrl("https://example.com/doc.pdf");

        assertThat(file.getName()).isEqualTo("Doc");
        assertThat(file.getType()).isEqualTo("pdf");
        assertThat(file.getUrl()).contains("doc.pdf");
    }

    @Test
    void fileRefStoresFields() {
        FileRef ref = new FileRef();
        ref.setName("Guide");
        ref.setType("pdf");
        ref.setUrl("https://example.com/guide.pdf");

        assertThat(ref.getName()).isEqualTo("Guide");
        assertThat(ref.getType()).isEqualTo("pdf");
        assertThat(ref.getUrl()).contains("guide");
    }

    @Test
    void likeStoresFields() {
        Like like = new Like("u1", null, "COURSE", "c1");

        assertThat(like.getTargetType()).isEqualTo("COURSE");
        assertThat(like.getTargetId()).isEqualTo("c1");
    }

    @Test
    void newsArticleStoresFields() {
        NewsArticle article = new NewsArticle();
        article.setTitle("Title");
        article.setSource("Source");
        article.setCountry("GH");

        assertThat(article.getTitle()).isEqualTo("Title");
        assertThat(article.getCountry()).isEqualTo("GH");
    }

    @Test
    void textContentTranslationStoresFields() {
        TextContentTranslation translation = new TextContentTranslation();
        translation.setTitle("Lesson");
        translation.setContent("Body");

        assertThat(translation.getTitle()).isEqualTo("Lesson");
        assertThat(translation.getContent()).isEqualTo("Body");
    }

    @Test
    void quizQuestionTranslationStoresFields() {
        QuizQuestionTranslation translation = new QuizQuestionTranslation();
        translation.setQuestion("Question?");

        assertThat(translation.getQuestion()).isEqualTo("Question?");
    }

    @Test
    void quizAnswerTranslationStoresFields() {
        QuizAnswerTranslation translation = new QuizAnswerTranslation();
        translation.setText("Answer");

        assertThat(translation.getText()).isEqualTo("Answer");
    }
}
