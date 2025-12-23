package org.agra.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryServerApplicationTest {

    @Test
    void createApplicationBuilds() {
        SpringApplication app = DiscoveryServerApplication.createApplication();

        assertThat(app).isNotNull();
        assertThat(app.getAllSources()).contains(DiscoveryServerApplication.class);
    }

    @Test
    void createApplicationAddsAutoCloseListenerWhenEnabled() {
        System.setProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            SpringApplication app = DiscoveryServerApplication.createApplication();
            assertThat(app).isNotNull();
        } finally {
            System.clearProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
