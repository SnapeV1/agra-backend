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
}
