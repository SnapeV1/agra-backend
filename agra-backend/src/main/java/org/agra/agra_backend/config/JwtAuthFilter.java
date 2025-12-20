package org.agra.agra_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        String token = resolveBearerToken(authHeader, path);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            authenticate(token, path, request);
        } catch (SignatureException se) {
            log.warn("JWT signature mismatch for path={}. Authorization='{}' token='{}'", request.getRequestURI(), authHeader, token);
        } catch (JwtException je) {
            log.warn("JWT parsing/validation failed for path={}: {}. token='{}'", request.getRequestURI(), je.getMessage(), token);
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication for path={}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(String authHeader, String path) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No/invalid Authorization header for path={}, header={}", path, authHeader);
            return null;
        }

        String token = authHeader.substring(7);
        if (token.isBlank() || "null".equalsIgnoreCase(token)) {
            log.warn("JWT header provided but token missing/blank for path={}. Authorization='{}' token='{}'", path, authHeader, token);
            return null;
        }
        return token;
    }

    private void authenticate(String token, String path, HttpServletRequest request) {
        String userId = jwtUtil.extractUserId(token);
        log.debug("JWT parsed for path={}, userId={}", path, userId);
        if (userId == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !jwtUtil.isTokenValid(token, user)) {
            log.debug("JWT token valid=false or user not found. path={}, userId={}", request.getRequestURI(), userId);
            return;
        }
        String userRole = resolveRole(user.getRole());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + userRole)));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("JWT accepted for path={}, userId={}, role={}", path, userId, userRole);
    }

    private String resolveRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "FARMER";
        }
        return role;
    }
}
