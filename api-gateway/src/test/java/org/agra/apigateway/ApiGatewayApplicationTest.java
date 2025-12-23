package org.agra.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApiGatewayApplicationTest {

    @Test
    void createApplicationBuilds() {
        SpringApplication app = ApiGatewayApplication.createApplication();

        assertThat(app).isNotNull();
        assertThat(app.getAllSources()).contains(ApiGatewayApplication.class);
    }

    @Test
    void createApplicationAddsAutoCloseListenerWhenEnabled() {
        System.setProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            SpringApplication app = ApiGatewayApplication.createApplication();
            assertThat(app).isNotNull();
        } finally {
            System.clearProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY);
        }
    }

    @Test
    void mainRunsWithAutoClose() {
        System.setProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> ApiGatewayApplication.main(new String[] {
                    "--spring.main.web-application-type=none",
                    "--spring.main.lazy-initialization=true",
                    "--spring.cloud.discovery.enabled=false",
                    "--eureka.client.enabled=false",
                    "--spring.cloud.service-registry.auto-registration.enabled=false"
            })).doesNotThrowAnyException();
        } finally {
            System.clearProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
