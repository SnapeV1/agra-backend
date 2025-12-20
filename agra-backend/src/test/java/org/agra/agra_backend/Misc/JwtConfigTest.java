package org.agra.agra_backend.Misc;

import org.agra.agra_backend.misc.JwtConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Disabled in CI")
class JwtConfigTest {

    @Test
    void getSecretReturnsValueFromPropertiesWhenConfigured() {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "propertySecret", "configured-secret");
        ReflectionTestUtils.setField(config, "environmentSecret", null);

        assertThat(config.getSecret()).isEqualTo("configured-secret");
    }

    @Test
    void getSecretFallsBackToEnvironmentWhenPropertyMissing() {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "propertySecret", " ");
        ReflectionTestUtils.setField(config, "environmentSecret", "env-secret");

        assertThat(config.getSecret()).isEqualTo("env-secret");
    }

    @Test
    void getSecretThrowsHelpfulExceptionWhenNotConfigured() {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "propertySecret", null);
        ReflectionTestUtils.setField(config, "environmentSecret", "  ");

        assertThatThrownBy(config::getSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }
}
