package org.agra.agra_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DebugRequestLoggingFilterTest {

    @Test
    void filterPassesThroughWithoutOrigin() throws ServletException, IOException {
        DebugRequestLoggingFilter filter = new DebugRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, new FlaggingFilterChain(invoked));

        assertThat(invoked.get()).isTrue();
    }

    @Test
    void filterLogsWhenOriginPresent() throws ServletException, IOException {
        DebugRequestLoggingFilter filter = new DebugRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/test");
        request.addHeader("Origin", "https://example.com");
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "Authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, new FlaggingFilterChain(invoked));

        assertThat(invoked.get()).isTrue();
    }

    private static class FlaggingFilterChain implements FilterChain {
        private final AtomicBoolean invoked;

        private FlaggingFilterChain(AtomicBoolean invoked) {
            this.invoked = invoked;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) {
            invoked.set(true);
        }
    }
}
