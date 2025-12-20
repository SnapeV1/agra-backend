package org.agra.agra_backend.misc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    @Test
    void getSecretPrefersPropertySecret() {
        JwtConfig config = new JwtConfig();
        setField(config, "propertySecret", "prop-secret");
        setField(config, "environmentSecret", "env-secret");

        assertThat(config.getSecret()).isEqualTo("prop-secret");
    }

    @Test
    void getSecretFallsBackToEnvironmentSecret() {
        JwtConfig config = new JwtConfig();
        setField(config, "propertySecret", " ");
        setField(config, "environmentSecret", "env-secret");

        assertThat(config.getSecret()).isEqualTo("env-secret");
    }

    @Test
    void getSecretThrowsWhenMissing() {
        JwtConfig config = new JwtConfig();
        setField(config, "propertySecret", " ");
        setField(config, "environmentSecret", null);

        assertThatThrownBy(config::getSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set field for test", e);
        }
    }
}
