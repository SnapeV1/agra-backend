package org.agra.agra_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    @Test
    void mainRunsWithAutoClose() {
        System.setProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY, "true");
        try {
            assertThatCode(() -> AgraBackendApplication.main(new String[] {
                    "--spring.main.web-application-type=none",
                    "--spring.main.lazy-initialization=true",
                    "--spring.cloud.discovery.enabled=false",
                    "--eureka.client.enabled=false",
                    "--spring.cloud.service-registry.auto-registration.enabled=false",
                    "--spring.data.mongodb.repositories.enabled=false",
                    "--spring.autoconfigure.exclude="
                            + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration,"
                            + "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration,"
                            + "org.springframework.cloud.netflix.eureka.loadbalancer.LoadBalancerEurekaAutoConfiguration"
            })).doesNotThrowAnyException();
        } finally {
            System.clearProperty(AgraBackendApplication.AUTO_CLOSE_PROPERTY);
        }
    }
}
