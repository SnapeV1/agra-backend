package org.agra.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayApplicationTest {

    @Test
    void createApplicationBuilds() {
        SpringApplication app = ApiGatewayApplication.createApplication();

        assertThat(app).isNotNull();
        assertThat(app.getAllSources()).contains(ApiGatewayApplication.class);
    }
}
