package org.agra.agra_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;

class AgraBackendApplicationTest {

    @Test
    void createApplicationBuilds() {
        SpringApplication app = AgraBackendApplication.createApplication();

        assertThat(app).isNotNull();
        assertThat(app.getAllSources()).contains(AgraBackendApplication.class);
    }

    @Test
    void createApplicationAddsAutoCloseListenerWhenEnabled() {
        System.setProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            SpringApplication app = AgraBackendApplication.createApplication();
            assertThat(app).isNotNull();
        } finally {
            System.clearProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
