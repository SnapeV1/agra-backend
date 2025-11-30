package org.agra.agra_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DebugRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DebugRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        String acrm = request.getHeader("Access-Control-Request-Method");
        String acrh = request.getHeader("Access-Control-Request-Headers");

        if (origin != null) {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.debug("CORS preflight: method={}, origin={}, ACRM={}, ACRH={}",
                        request.getMethod(), origin, acrm, acrh);
            } else {
                log.debug("CORS request: method={}, origin={}, path={}",
                        request.getMethod(), origin, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);

        if (origin != null) {
            String acao = response.getHeader("Access-Control-Allow-Origin");
            String acc = response.getHeader("Access-Control-Allow-Credentials");
            String acam = response.getHeader("Access-Control-Allow-Methods");
            String acah = response.getHeader("Access-Control-Allow-Headers");
            log.debug("CORS response headers: ACAO={}, ACC={}, ACAM={}, ACAH={}", acao, acc, acam, acah);
        }
    }
}

