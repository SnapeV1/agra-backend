package org.agra.agra_backend.model;

import org.agra.agra_backend.payload.UserInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        assertThat(like.getActive()).isTrue();
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
    void quizAnswerStoresTranslations() {
        QuizAnswer answer = new QuizAnswer();
        answer.setText(Map.of("en", "Option A"));

        assertThat(answer.getText()).containsEntry("en", "Option A");
    }

    @Test
    void quizAnswerAllArgsConstructorStoresFields() {
        QuizAnswer answer = new QuizAnswer("a1", Map.of("en", "A"), true);

        assertThat(answer.getId()).isEqualTo("a1");
        assertThat(answer.isCorrect()).isTrue();
        assertThat(answer.getText()).containsKey("en");
    }

    @Test
    void quizQuestionAcceptsAnswers() {
        QuizQuestion question = new QuizQuestion();
        question.setAnswers(List.of(new QuizAnswer()));

        assertThat(question.getAnswers()).hasSize(1);
    }

    @Test
    void quizQuestionStoresTranslations() {
        QuizQuestion question = new QuizQuestion();
        question.setQuestion(Map.of("en", "Q?"));

        assertThat(question.getQuestion()).containsEntry("en", "Q?");
    }

    @Test
    void quizQuestionAllArgsConstructorStoresFields() {
        QuizQuestion question = new QuizQuestion("q1", Map.of("en", "Q?"), List.of(new QuizAnswer()));

        assertThat(question.getId()).isEqualTo("q1");
        assertThat(question.getAnswers()).hasSize(1);
    }

    @Test
    void textContentStoresQuestions() {
        TextContent content = new TextContent();
        content.setQuizQuestions(List.of(new QuizQuestion()));

        assertThat(content.getQuizQuestions()).hasSize(1);
    }

    @Test
    void textContentStoresTranslations() {
        TextContent content = new TextContent();
        content.setTitle(Map.of("en", "Lesson"));

        assertThat(content.getTitle()).containsEntry("en", "Lesson");
    }

    @Test
    void textContentAllArgsConstructorStoresFields() {
        TextContent content = new TextContent("t1", 1, "QUIZ", Map.of("en", "Lesson"), Map.of("en", "Body"),
                List.of(new QuizQuestion()));

        assertThat(content.getId()).isEqualTo("t1");
        assertThat(content.getOrder()).isEqualTo(1);
        assertThat(content.getType()).isEqualTo("QUIZ");
        assertThat(content.getQuizQuestions()).hasSize(1);
    }

    @Test
    void trainingKitStoresFields() {
        TrainingKit kit = new TrainingKit();
        kit.setTitle("Guide");
        kit.setCourseId("course-1");
        kit.setLanguage("en");
        kit.setUploadDate(new Date());

        assertThat(kit.getTitle()).isEqualTo("Guide");
        assertThat(kit.getCourseId()).isEqualTo("course-1");
        assertThat(kit.getLanguage()).isEqualTo("en");
    }

    @Test
    void ticketMessageStoresFlags() {
        TicketMessage message = new TicketMessage();
        message.setAdminMessage(true);
        message.setContent("Reply");

        assertThat(message.isAdminMessage()).isTrue();
        assertThat(message.getContent()).isEqualTo("Reply");
    }

    @Test
    void userRoleIncludesAdmin() {
        assertThat(UserRole.valueOf("ADMIN")).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void refreshTokenStoresFields() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("refresh-1");
        token.setUserId("user-1");
        token.setRevoked(true);

        assertThat(token.getTokenHash()).isEqualTo("refresh-1");
        assertThat(token.getUserId()).isEqualTo("user-1");
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void refreshTokenReportsNotExpiredWhenMissingDate() {
        RefreshToken token = new RefreshToken();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void emailVerificationTokenStoresFields() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setTokenHash("verify-1");
        token.setUserId("user-1");

        assertThat(token.getTokenHash()).isEqualTo("verify-1");
        assertThat(token.getUserId()).isEqualTo("user-1");
    }

    @Test
    void emailVerificationTokenReportsExpired() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setExpirationDate(new Date(System.currentTimeMillis() - 1000));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void userStoresProfileFields() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user@example.com");
        user.setRole("ADMIN");
        user.setThemePreference("dark");
        user.setTwoFactorEnabled(true);

        assertThat(user.getId()).isEqualTo("user-1");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.getThemePreference()).isEqualTo("dark");
        assertThat(user.getTwoFactorEnabled()).isTrue();
    }

    @Test
    void sessionStoresScheduleFields() {
        Session session = new Session();
        session.setId("s1");
        session.setCourseId("course-1");
        session.setTitle("Live Session");
        session.setRoomName("room-1");
        session.setLobbyEnabled(false);
        session.setRecordingEnabled(true);
        session.setWatchSecondsByUserId(Map.of("u1", 30L));

        assertThat(session.getId()).isEqualTo("s1");
        assertThat(session.getCourseId()).isEqualTo("course-1");
        assertThat(session.getTitle()).isEqualTo("Live Session");
        assertThat(session.getRoomName()).isEqualTo("room-1");
        assertThat(session.getLobbyEnabled()).isFalse();
        assertThat(session.getRecordingEnabled()).isTrue();
        assertThat(session.getWatchSecondsByUserId()).containsEntry("u1", 30L);
    }

    @Test
    void commentLikeStoresFields() {
        CommentLike like = new CommentLike("id", "user-1", "comment-1", LocalDateTime.now(), true);

        assertThat(like.getUserId()).isEqualTo("user-1");
        assertThat(like.getCommentId()).isEqualTo("comment-1");
        assertThat(like.getActive()).isTrue();
    }

    @Test
    void commentConstructorsSetDefaults() {
        UserInfo info = new UserInfo();
        Comment comment = new Comment("post-1", "user-1", info, "Hello");
        Comment reply = new Comment("post-1", "user-2", info, "Reply", "parent-1", "user-1");

        assertThat(comment.getLikesCount()).isZero();
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(reply.getParentCommentId()).isEqualTo("parent-1");
        assertThat(reply.getReplyToUserId()).isEqualTo("user-1");
    }

    @Test
    void postStoresDefaultsAndUserInfo() {
        Post post = new Post();
        post.setUserId("user-1");
        post.setUserInfo(new UserInfo("u1", "User", "e", "pic", new Date()));

        assertThat(post.getCommentIds()).isNotNull();
        assertThat(post.getCommentsCount()).isZero();
        assertThat(post.getLikesCount()).isZero();
        assertThat(post.getUserInfo().getName()).isEqualTo("User");
    }

    @Test
    void postAllArgsConstructorStoresFields() {
        Post post = new Post(
                "p1",
                "u1",
                new UserInfo("u1", "User", "e", "pic", new Date()),
                "content",
                "img",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                "course-1",
                List.of("c1"),
                2L,
                3L,
                null,
                true
        );

        assertThat(post.getId()).isEqualTo("p1");
        assertThat(post.getIsCoursePost()).isTrue();
        assertThat(post.getCommentsCount()).isEqualTo(2L);
        assertThat(post.getLikesCount()).isEqualTo(3L);
    }

    @Test
    void postSettersAndGettersCoverAllFields() {
        Post post = new Post();
        post.setId("p2");
        post.setUserId("u2");
        post.setUserInfo(new UserInfo("u2", "User2", "e2", "pic2", new Date()));
        post.setContent("content");
        post.setImageUrl("img");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setIsCoursePost(false);
        post.setCourseId("course-2");
        post.setCommentIds(List.of("c1"));
        post.setCommentsCount(1L);
        post.setLikesCount(2L);
        post.setComments(List.of(new Comment()));
        post.setIsLikedByCurrentUser(true);

        assertThat(post.getId()).isEqualTo("p2");
        assertThat(post.getUserId()).isEqualTo("u2");
        assertThat(post.getUserInfo().getName()).isEqualTo("User2");
        assertThat(post.getContent()).isEqualTo("content");
        assertThat(post.getImageUrl()).isEqualTo("img");
        assertThat(post.getCreatedAt()).isNotNull();
        assertThat(post.getUpdatedAt()).isNotNull();
        assertThat(post.getIsCoursePost()).isFalse();
        assertThat(post.getCourseId()).isEqualTo("course-2");
        assertThat(post.getCommentIds()).containsExactly("c1");
        assertThat(post.getCommentsCount()).isEqualTo(1L);
        assertThat(post.getLikesCount()).isEqualTo(2L);
        assertThat(post.getComments()).hasSize(1);
        assertThat(post.getIsLikedByCurrentUser()).isTrue();
    }
    @Test
    void quizAnswerSettersAndGetters() {
        QuizAnswer answer = new QuizAnswer();
        answer.setId("a1");
        answer.setText(Map.of("en", "Answer"));
        answer.setCorrect(false);

        assertThat(answer.getId()).isEqualTo("a1");
        assertThat(answer.getText()).containsKey("en");
        assertThat(answer.isCorrect()).isFalse();
    }

    @Test
    void quizQuestionSettersAndGetters() {
        QuizQuestion question = new QuizQuestion();
        question.setId("q1");
        question.setQuestion(Map.of("en", "Q1"));
        question.setAnswers(List.of(new QuizAnswer()));

        assertThat(question.getId()).isEqualTo("q1");
        assertThat(question.getQuestion()).containsKey("en");
        assertThat(question.getAnswers()).hasSize(1);
    }

    @Test
    void textContentSettersAndGetters() {
        TextContent content = new TextContent();
        content.setId("t1");
        content.setOrder(2);
        content.setType("QUIZ");
        content.setTitle(Map.of("en", "Title"));
        content.setQuizQuestions(List.of(new QuizQuestion()));

        assertThat(content.getId()).isEqualTo("t1");
        assertThat(content.getOrder()).isEqualTo(2);
        assertThat(content.getType()).isEqualTo("QUIZ");
        assertThat(content.getTitle()).containsKey("en");
        assertThat(content.getQuizQuestions()).hasSize(1);
    }

    @Test
    void adminSettingsConstructorsAndFields() {
        AdminSettings settings = new AdminSettings("id");
        settings.setNewsCron("0 0 9 ? * MON");
        settings.setNewsFetchCooldownSeconds(60);
        settings.setLastNewsFetchAt(java.time.Instant.now());
        settings.setTwoFactorEnforced(true);
        settings.setAdminEmail("admin@example.com");

        assertThat(settings.getId()).isEqualTo("id");
        assertThat(settings.getNewsCron()).isEqualTo("0 0 9 ? * MON");
        assertThat(settings.getNewsFetchCooldownSeconds()).isEqualTo(60);
        assertThat(settings.getLastNewsFetchAt()).isNotNull();
        assertThat(settings.getTwoFactorEnforced()).isTrue();
        assertThat(settings.getAdminEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void adminSettingsNoArgsConstructorStoresFields() {
        AdminSettings settings = new AdminSettings();
        settings.setId("id-2");
        settings.setNewsCron("cron");
        settings.setNewsFetchCooldownSeconds(30);
        settings.setLastNewsFetchAt(java.time.Instant.now());
        settings.setTwoFactorEnforced(false);
        settings.setAdminEmail("root@example.com");

        assertThat(settings.getId()).isEqualTo("id-2");
        assertThat(settings.getNewsCron()).isEqualTo("cron");
        assertThat(settings.getNewsFetchCooldownSeconds()).isEqualTo(30);
        assertThat(settings.getLastNewsFetchAt()).isNotNull();
        assertThat(settings.getTwoFactorEnforced()).isFalse();
        assertThat(settings.getAdminEmail()).isEqualTo("root@example.com");
    }

    @Test
    void notificationPreferencesDigestSettingsStoresFields() {
        NotificationPreferences.DigestSettings digest = new NotificationPreferences.DigestSettings();
        digest.setEnabled(true);
        digest.setFrequency("weekly");

        assertThat(digest.isEnabled()).isTrue();
        assertThat(digest.getFrequency()).isEqualTo("weekly");
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
    void likeAllArgsConstructorStoresFields() {
        Like like = new Like("id", "user-1", new User(), "POST", "p1", LocalDateTime.now());

        assertThat(like.getId()).isEqualTo("id");
        assertThat(like.getUserId()).isEqualTo("user-1");
        assertThat(like.getTargetType()).isEqualTo("POST");
        assertThat(like.getTargetId()).isEqualTo("p1");
        assertThat(like.getUserInfo()).isNotNull();
    }

    @Test
    void activityLogStoresFields() {
        ActivityLog log = new ActivityLog();
        log.setId("log-1");
        log.setUserId("user-1");
        log.setActivityType(ActivityType.LIKE);
        log.setAction("Liked post");
        log.setTargetType("POST");
        log.setTargetId("post-1");
        log.setMetadata(Map.of("postId", "post-1"));
        log.setCreatedAt(LocalDateTime.now());

        assertThat(log.getId()).isEqualTo("log-1");
        assertThat(log.getActivityType()).isEqualTo(ActivityType.LIKE);
        assertThat(log.getTargetType()).isEqualTo("POST");
        assertThat(log.getMetadata()).containsEntry("postId", "post-1");
    }

    @Test
    void activityTypeIncludesCourseCompletion() {
        assertThat(ActivityType.valueOf("COURSE_COMPLETION")).isEqualTo(ActivityType.COURSE_COMPLETION);
    }

    @Test
    void adminAuditLogStoresFields() {
        AdminAuditLog log = new AdminAuditLog();
        log.setId("audit-1");
        log.setAdminUserId("admin-1");
        log.setAction("ACTIVITY_LOG_QUERY");
        log.setMetadata(Map.of("userId", "u1"));
        log.setCreatedAt(LocalDateTime.now());

        assertThat(log.getId()).isEqualTo("audit-1");
        assertThat(log.getAdminUserId()).isEqualTo("admin-1");
        assertThat(log.getAction()).isEqualTo("ACTIVITY_LOG_QUERY");
        assertThat(log.getMetadata()).containsEntry("userId", "u1");
    }

    @Test
    void activityLogAllArgsConstructorStoresFields() {
        ActivityLog log = new ActivityLog(
                "log-2",
                "user-1",
                new UserInfo("u1", "User", "u@example.com", "pic", new Date()),
                ActivityType.LIKE,
                "Liked post",
                "POST",
                "post-1",
                Map.of("postId", "post-1"),
                LocalDateTime.now()
        );

        assertThat(log.getId()).isEqualTo("log-2");
        assertThat(log.getAction()).isEqualTo("Liked post");
        assertThat(log.getTargetType()).isEqualTo("POST");
        assertThat(log.getUserInfo().getEmail()).isEqualTo("u@example.com");
    }

    @Test
    void adminAuditLogAllArgsConstructorStoresFields() {
        AdminAuditLog log = new AdminAuditLog(
                "audit-2",
                "admin-1",
                new UserInfo("admin-1", "Admin", "admin@example.com", "pic", new Date()),
                "ACTIVITY_LOG_QUERY",
                Map.of("reason", "audit"),
                LocalDateTime.now()
        );

        assertThat(log.getId()).isEqualTo("audit-2");
        assertThat(log.getAction()).isEqualTo("ACTIVITY_LOG_QUERY");
        assertThat(log.getAdminInfo().getName()).isEqualTo("Admin");
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
    void notificationPreferencesDefaults() {
        NotificationPreferences prefs = new NotificationPreferences();

        assertThat(prefs.isNotificationsEnabled()).isTrue();
        assertThat(prefs.getChannels()).contains("email", "websocket");
        assertThat(prefs.getDigest()).isNotNull();
        assertThat(prefs.getDigest().isEnabled()).isFalse();
    }

    @Test
    void ticketDefaultsStatus() {
        Ticket ticket = new Ticket();
        ticket.setSubject("Help");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void courseFileAllArgsConstructorStoresFields() {
        CourseFile file = new CourseFile("id", "Doc", "pdf", "url", "public", 12L, new Date());

        assertThat(file.getId()).isEqualTo("id");
        assertThat(file.getType()).isEqualTo("pdf");
        assertThat(file.getSize()).isEqualTo(12L);
    }
}
