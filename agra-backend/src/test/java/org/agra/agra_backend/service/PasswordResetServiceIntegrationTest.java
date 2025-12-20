package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.PasswordResetTokenRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.PasswordResetToken;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;

@Tag("integration")
@Disabled("Disabled in CI")
@DataMongoTest(properties = "eureka.client.enabled=false")
@Import(PasswordResetService.class)
@ActiveProfiles("test")
class PasswordResetServiceIntegrationTest {

    private static TransitionWalker.ReachedState<RunningMongodProcess> embeddedMongo;
    private static int mongoPort;

    @BeforeAll
    static void startEmbeddedMongo() {
        embeddedMongo = Mongod.instance().start(Version.V6_0_5);
        mongoPort = embeddedMongo.current().getServerAddress().getPort();
    }

    @AfterAll
    static void stopEmbeddedMongo() {
        if (embeddedMongo != null) {
            embeddedMongo.close();
        }
    }

    @DynamicPropertySource
    static void configureMongoUri(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:" + mongoPort + "/agra-backend-test");
    }

    @Autowired
    private PasswordResetService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    // External dependencies mocked so we can focus on data + security wiring
    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void issueResetTokenPersistsHashedTokenWithEmbeddedMongo() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("user1@example.com");
        userRepository.save(user);

        String rawToken = service.issueResetTokenForUserId("user-1");

        PasswordResetToken saved = tokenRepository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getTokenHash()).isEqualTo(hash(rawToken));
        assertThat(saved.getExpirationDate()).isAfter(saved.getCreatedAt());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void resetPasswordUpdatesUserAndDeletesToken() {
        User user = new User();
        user.setId("user-2");
        user.setEmail("user2@example.com");
        userRepository.save(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash("raw-token"));
        token.setCreatedAt(new Date());
        token.setExpirationDate(minutesFromNow(5));
        tokenRepository.save(token);

        when(passwordEncoder.encode("newPass!")).thenReturn("ENCODED");

        service.resetPassword("raw-token", "newPass!");

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPassword()).isEqualTo("ENCODED");
        assertThat(tokenRepository.findByTokenHash(hash("raw-token"))).isEmpty();
        verify(passwordEncoder).encode("newPass!");
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token for test", e);
        }
    }

    private static Date minutesFromNow(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }
}
