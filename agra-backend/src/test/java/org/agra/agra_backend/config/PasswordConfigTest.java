package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTest {

    @Test
    void passwordEncoderReturnsBcrypt() {
        PasswordConfig config = new PasswordConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("secret");
        assertThat(encoded).isNotBlank();
        assertThat(encoder.matches("secret", encoded)).isTrue();
    }
}
