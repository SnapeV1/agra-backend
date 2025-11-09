package org.agra.agra_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOriginsCsv;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }


    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Admin endpoints (avoid PathPattern "**" mid-path which is invalid)
                        .requestMatchers("/api/courses/admin/**").hasRole("ADMIN")
                        // Specific admin-only operations
                        .requestMatchers(HttpMethod.POST, "/api/news/fetch-weekly").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/courses/addCourse").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/updateCourse/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/ArchiveCourse/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/delete/*").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/courses/*/enrollment-status").authenticated()
                        .requestMatchers("/api/courses/*/enroll").authenticated()
                        .requestMatchers("/api/courses/enrolled").authenticated()
                        .requestMatchers("/api/progress/**").authenticated()
                        .requestMatchers("/api/courses/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/cloudinary/**").permitAll()
                        .requestMatchers("/api/sessions/**").permitAll()
                        .requestMatchers("/api/news/**").permitAll()
                        // WebSocket/STOMP endpoints
                        .requestMatchers("/ws/**", "/ws-sockjs/**").permitAll()
                        // Notifications: public list, but unseen/mark-seen require auth
                        .requestMatchers(HttpMethod.DELETE, "/api/notifications").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/notifications/delete").authenticated()
                        .requestMatchers("/api/notifications/me").authenticated()
                        .requestMatchers("/api/notifications/unseen").authenticated()
                        .requestMatchers("/api/notifications/*/seen").authenticated()
                        .requestMatchers("/api/notifications/mark-all-seen").authenticated()
                        .requestMatchers("/api/notifications/**").permitAll()


                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Use explicit origins when credentials are enabled. Wildcard is invalid with credentials.
        List<String> allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
