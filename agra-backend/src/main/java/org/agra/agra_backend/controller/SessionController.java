package org.agra.agra_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.UserRole;
import org.agra.agra_backend.payload.AttendanceEventDto;
import org.agra.agra_backend.payload.CreateSessionDto;
import org.agra.agra_backend.payload.JoinResponse;
import org.agra.agra_backend.service.SessionService;
import org.agra.agra_backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService service;
    private final UserService userService;

    @Value("${domain}")
    private String jitsiDomain;

    @PostMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Session create(@PathVariable String courseId, @Valid @RequestBody CreateSessionDto dto) {
        return service.create(courseId, dto);
    }

    @GetMapping("/courses/{courseId}/upcoming")
    @PreAuthorize("isAuthenticated()")
    public List<Session> upcoming(@PathVariable String courseId) {
        var user = userService.getCurrentUserOrThrow();
        boolean moderator = isAdmin(user.getRole());
        return service.upcoming(courseId, user.getId(), moderator);
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public Session get(@PathVariable String sessionId) {
        return service.getById(sessionId);
    }

    @PostMapping("/{sessionId}/join")
    @PreAuthorize("isAuthenticated()")
    public JoinResponse join(@PathVariable String sessionId) {
        var user = userService.getCurrentUserOrThrow();
        boolean moderator = isAdmin(user.getRole());
        System.out.println("[JITSI][Controller] Join requested. sessionId=" + sessionId);
        System.out.println("[JITSI][Controller] Current user: id=" + user.getId() + ", name=" + user.getName() + ", role=" + user.getRole() + ", moderator=" + moderator);
        var resp = service.join(sessionId, user, moderator);
        System.out.println("[JITSI][Controller] Service join response: room=" + resp.getRoomName() + ", tokenPresent=" + (resp.getJwt() != null) + ", displayName=" + resp.getDisplayName());
        String normalizedDomain = normalizeHost(jitsiDomain);
        resp.setDomain(normalizedDomain); 
        String https8443 = normalizedDomain == null || normalizedDomain.isEmpty() ? "N/A" : "https://" + normalizedDomain + ":8443";
        System.out.println("[JITSI][Controller] Setting domain on response: raw=" + jitsiDomain + ", normalized=" + normalizedDomain + ", expectedBaseUrl(https:8443)=" + https8443);
        return resp;
    }

    private static boolean isAdmin(String role) {
        if (role == null) return false;
        String r = role.trim();
        return "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r);
    }

    @PostMapping("/{sessionId}/events")
    @PreAuthorize("isAuthenticated()")
    public void event(@PathVariable String sessionId, @RequestBody AttendanceEventDto dto) {
        service.recordEvent(sessionId, userService.getCurrentUserOrThrow().getId(), dto);
    }

    /**
     * Returns a bare host (no scheme/port/path) to keep Jitsi domain and sub consistent, e.g. "https://jitsi.local:8443" -> "jitsi.local".
     */
    private static String normalizeHost(String raw) {
        if (raw == null) return null;
        String val = raw.trim();
        if (val.startsWith("http://")) val = val.substring("http://".length());
        else if (val.startsWith("https://")) val = val.substring("https://".length());
        if (val.contains("/")) val = val.substring(0, val.indexOf("/"));
        if (val.contains(":")) val = val.substring(0, val.indexOf(":"));
        return val;
    }
}
