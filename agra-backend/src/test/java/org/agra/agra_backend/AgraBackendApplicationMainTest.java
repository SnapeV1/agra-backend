package org.agra.agra_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AgraBackendApplicationMainTest {

    @Test
    void createApplicationBuilds() {
        SpringApplication app = AgraBackendApplication.createApplication();

        assertThat(app).isNotNull();
        assertThat(app.getAllSources()).contains(AgraBackendApplication.class);
    }

    @Test
    void mainRunsWithAutoClose() {
        System.setProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> AgraBackendApplication.main(
                    new String[] {"--spring.main.web-application-type=none"}))
                    .doesNotThrowAnyException();
        } finally {
            System.clearProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
