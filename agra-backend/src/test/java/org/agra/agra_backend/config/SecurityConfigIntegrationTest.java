package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = SecurityConfigIntegrationTest.TestApp.class,
        properties = {
                "cors.allowed-origins=http://a.com, http://b.com",
                "security.csp.directives=default-src 'self'",
                "spring.cloud.discovery.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                        + "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration,"
                        + "org.springframework.cloud.netflix.eureka.loadbalancer.LoadBalancerEurekaAutoConfiguration"
        }
)
class SecurityConfigIntegrationTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void securityBeansAreCreated() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void corsConfigurationUsesProvidedOrigins() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://a.com", "http://b.com");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApp {
        @Bean
        JwtAuthFilter jwtAuthFilter() {
            return mock(JwtAuthFilter.class);
        }
    }
}
