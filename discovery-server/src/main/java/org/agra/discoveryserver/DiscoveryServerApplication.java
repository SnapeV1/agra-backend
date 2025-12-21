package org.agra.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        return new SpringApplication(DiscoveryServerApplication.class);
    }
}
