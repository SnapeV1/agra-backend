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
    void mainRunsWithAutoClose() {
        System.setProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> DiscoveryServerApplication.main(new String[] {
                    "--server.port=0",
                    "--eureka.client.register-with-eureka=false",
                    "--eureka.client.fetch-registry=false"
            })).doesNotThrowAnyException();
        } finally {
            System.clearProperty(DiscoveryServerApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
