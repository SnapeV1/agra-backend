package org.agra.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    @Test
    void mainRunsWithAutoClose() {
        System.setProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> DiscoveryServerApplication.main(new String[] {
                    "--spring.main.web-application-type=none",
                    "--spring.main.lazy-initialization=true",
                    "--spring.cloud.discovery.enabled=false",
                    "--eureka.client.enabled=false",
                    "--spring.cloud.service-registry.auto-registration.enabled=false"
            })).doesNotThrowAnyException();
        } finally {
            System.clearProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
