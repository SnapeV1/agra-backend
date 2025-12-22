package org.agra.agra_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class AgraBackendApplication {

    static final String AUTO_CLOSE_PROPERTY = "app.test.auto-close";

    public static void main(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        SpringApplication app = new SpringApplication(AgraBackendApplication.class);
        if (Boolean.getBoolean(AUTO_CLOSE_PROPERTY)) {
            app.addListeners(event -> {
                if (event instanceof ApplicationReadyEvent readyEvent) {
                    readyEvent.getApplicationContext().close();
                }
            });
        }
        return app;
    }

}
