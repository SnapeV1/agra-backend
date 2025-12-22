package org.agra.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication
public class ApiGatewayApplication {

    static final String AUTO_CLOSE_PROPERTY = "app.test.auto-close";

    public static void main(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        SpringApplication app = new SpringApplication(ApiGatewayApplication.class);
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
