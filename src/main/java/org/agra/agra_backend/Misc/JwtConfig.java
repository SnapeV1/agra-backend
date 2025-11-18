package org.agra.agra_backend.Misc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:#{null}}")
    private String propertySecret;

    @Value("${JWT_SECRET:#{null}}")
    private String environmentSecret;

    public String getSecret() {
        String jwtSecret = resolveSecret();

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured. Please set 'jwt.secret' in application.properties or JWT_SECRET environment variable");
        }

        return jwtSecret;
    }

    private String resolveSecret() {
        if (propertySecret != null && !propertySecret.trim().isEmpty()) {
            System.out.println("Using JWT secret from properties file");
            return propertySecret;
        }

        if (environmentSecret != null && !environmentSecret.trim().isEmpty()) {
            System.out.println("Using JWT secret from environment variable");
            return environmentSecret;
        }

        return null;
    }
}
