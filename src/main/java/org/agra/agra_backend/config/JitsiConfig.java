package org.agra.agra_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JitsiConfig {

    @Value("${jitsi.domain:#{null}}")
    private String domainProp;

    @Value("${jitsi.app-id:#{null}}")
    private String appIdProp;

    @Value("${jitsi.app-secret:#{null}}")
    private String appSecretProp;

    @Value("${jitsi.audience:jitsi}")
    private String audienceProp;

    @Value("${jitsi.token-ttl-seconds:3600}")
    private int tokenTtlSecondsProp;

    public String getDomain() {
        return firstNonNull(domainProp, "JITSI_DOMAIN", "Jitsi domain not configured");
    }

    public String getAppId() {
        return firstNonNull(appIdProp, "JITSI_APP_ID", "Jitsi app-id not configured");
    }

    public String getAppSecret() {
        return firstNonNull(appSecretProp, "JITSI_APP_SECRET", "Jitsi app-secret not configured");
    }

    public String getAudience() {
        return firstNonNull(audienceProp, "JITSI_AUDIENCE", "jitsi"); // default jitsi
    }

    public int getTokenTtlSeconds() {
        String env = System.getenv("JITSI_TOKEN_TTL");
        if (env != null) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException ignored) {}
        }

        return tokenTtlSecondsProp;
    }

    /**
     * Helper method: return the first non-null, non-blank value
     * - checks the property first
     * - then environment variable
     * - if both missing, either return defaultValue or throw exception
     */
    private String firstNonNull(String propValue, String envVar, String defaultValueOrError) {
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        // if defaultValueOrError is literal default (like "jitsi") return it, else throw
        if (defaultValueOrError != null && !defaultValueOrError.isBlank() &&
                !defaultValueOrError.toLowerCase().contains("not configured")) {
            return defaultValueOrError;
        }
        throw new IllegalStateException(defaultValueOrError);
    }
}
