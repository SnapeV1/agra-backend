package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.AttendanceEventDto;
import org.agra.agra_backend.payload.CreateSessionDto;
import org.agra.agra_backend.payload.JoinResponse;
import org.agra.agra_backend.service.SessionService;
import org.agra.agra_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService service;
    @Mock
    private UserService userService;

    @InjectMocks
    private SessionController controller;

    @Test
    void upcomingUsesModeratorForAdmin() {
        User user = new User();
        user.setId("admin-1");
        user.setRole("ADMIN");
        when(userService.getCurrentUserOrThrow()).thenReturn(user);
        when(service.upcoming("course-1", "admin-1", true)).thenReturn(List.of());

        List<Session> response = controller.upcoming("course-1");

        assertThat(response).isEmpty();
        verify(service).upcoming("course-1", "admin-1", true);
    }

    @Test
    void joinSetsNormalizedDomain() throws Exception {
        User user = new User();
        user.setId("user-1");
        user.setName("User");
        user.setRole("USER");
        when(userService.getCurrentUserOrThrow()).thenReturn(user);

        JoinResponse joinResponse = new JoinResponse("room-1", null, "jwt", "User", null);
        when(service.join("session-1", user, false)).thenReturn(joinResponse);

        setField(controller, "jitsiDomain", "https://jitsi.local:8443");

        JoinResponse response = controller.join("session-1");

        assertThat(response.getDomain()).isEqualTo("jitsi.local");
    }

    @Test
    void createDelegatesToService() {
        CreateSessionDto dto = new CreateSessionDto();
        dto.setTitle("Title");
        dto.setStartTime(Instant.now());
        dto.setEndTime(Instant.now());

        Session session = new Session();
        when(service.create("course-1", dto)).thenReturn(session);

        Session response = controller.create("course-1", dto);

        assertThat(response).isSameAs(session);
    }

    @Test
    void eventDelegatesToService() {
        User user = new User();
        user.setId("user-1");
        when(userService.getCurrentUserOrThrow()).thenReturn(user);

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.JOIN);

        controller.event("session-1", dto);

        verify(service).recordEvent("session-1", "user-1", dto);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
