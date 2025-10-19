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
        System.out.println("moderator: '" + moderator + " " + user.getRole());
        var resp = service.join(sessionId, user, moderator);
        resp.setDomain(jitsiDomain); 
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
}
