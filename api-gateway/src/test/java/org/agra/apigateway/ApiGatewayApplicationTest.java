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
    void mainRunsWithAutoClose() {
        System.setProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> ApiGatewayApplication.main(
                    new String[] {
                            "--spring.main.web-application-type=reactive",
                            "--server.port=0",
                            "--spring.cloud.discovery.enabled=false",
                            "--spring.cloud.gateway.discovery.locator.enabled=false",
                            "--eureka.client.enabled=false"
                    }))
                    .doesNotThrowAnyException();
        } finally {
            System.clearProperty(ApiGatewayApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
