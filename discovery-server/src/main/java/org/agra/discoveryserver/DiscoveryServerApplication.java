package org.agra.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    static final String AUTO_CLOSE_PROPERTY = "app.test.auto-close";

    public static void main(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        SpringApplication app = new SpringApplication(DiscoveryServerApplication.class);
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
