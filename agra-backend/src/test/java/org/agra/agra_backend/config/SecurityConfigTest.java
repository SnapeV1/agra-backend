package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class SecurityConfigTest {

    @Test
    void corsConfigurationSourceBuilds() {
        JwtAuthFilter filter = mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(filter);
        ReflectionTestUtils.setField(config, "allowedOriginsCsv", "http://localhost:4200, https://example.com");

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isNotNull();
    }

    @Test
    void securityFilterChainBuilds() throws Exception {
        JwtAuthFilter filter = mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(filter);
        ReflectionTestUtils.setField(config, "contentSecurityPolicy", "default-src 'self'");
        org.springframework.security.config.annotation.web.builders.HttpSecurity http =
                mock(org.springframework.security.config.annotation.web.builders.HttpSecurity.class, RETURNS_DEEP_STUBS);
        org.springframework.security.web.DefaultSecurityFilterChain chain =
                mock(org.springframework.security.web.DefaultSecurityFilterChain.class);

        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.headers(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.securityFilterChain(http);

        assertThat(result).isSameAs(chain);
    }
}
