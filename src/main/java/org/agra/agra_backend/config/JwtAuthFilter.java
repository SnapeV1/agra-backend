package org.agra.agra_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.agra.agra_backend.Misc.JwtUtil;
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

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String userId = jwtUtil.extractUserId(token);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && jwtUtil.isTokenValid(token, user)) {
                    String userRole = user.getRole();
                    if (userRole == null || userRole.trim().isEmpty()) {
                        userRole = "FARMER";
                    }
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + userRole)));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.debug("JWT token valid=false or user not found. path={}, userId={}", request.getRequestURI(), userId);
                }
            }
        } catch (SignatureException se) {
            log.warn("JWT signature mismatch for request path={}. This often means a token signed with a different secret (e.g., Jitsi JWT) was sent as Authorization.", request.getRequestURI());
        } catch (JwtException je) {
            log.warn("JWT parsing/validation failed for path={}: {}", request.getRequestURI(), je.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication for path={}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
