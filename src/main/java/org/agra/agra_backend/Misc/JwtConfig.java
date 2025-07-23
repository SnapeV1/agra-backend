package org.agra.agra_backend.Misc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:#{null}}")
    private String secret;

    public String getSecret() {
        String jwtSecret = secret;

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            jwtSecret = System.getenv("JWT_SECRET");
            System.out.println("Using JWT secret from environment variable");
        } else {
            System.out.println("Using JWT secret from properties file");
        }

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Please set 'jwt.secret' in application.properties or JWT_SECRET environment variable");
        }

        return jwtSecret;
    }
}