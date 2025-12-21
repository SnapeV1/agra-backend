package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceWsControllerTest {

    @Mock
    private PresenceService presenceService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresenceWsController controller;

    @Test
    void heartbeatReturnsUserInfo() {
        User user = new User();
        user.setName("Alice");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        Principal principal = () -> "user-1";

        Map<String, Object> response = controller.heartbeat(null, "session-1", principal);

        assertThat(response.get("userId")).isEqualTo("user-1");
        assertThat(response.get("username")).isEqualTo("Alice");
        verify(presenceService).refresh("user-1", "session-1");
    }

    @Test
    void heartbeatReturnsAnonymousWhenNoPrincipal() {
        Map<String, Object> response = controller.heartbeat(null, "session-1", null);

        assertThat(response.get("userId")).isEqualTo("");
        assertThat(response.get("username")).isEqualTo("");
    }

    @Test
    void heartbeatFallsBackToUnknownName() {
        User user = new User();
        user.setName(" ");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        Principal principal = () -> "user-1";

        Map<String, Object> response = controller.heartbeat(null, "session-1", principal);

        assertThat(response.get("username")).isEqualTo("unknown");
    }
}
