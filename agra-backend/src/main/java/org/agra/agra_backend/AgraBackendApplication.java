package org.agra.agra_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class AgraBackendApplication {

    public static void main(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        return new SpringApplication(AgraBackendApplication.class);
    }

}
