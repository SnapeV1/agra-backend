package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.SessionRepository;
import org.agra.agra_backend.misc.JitsiTokenService;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.AttendanceEventDto;
import org.agra.agra_backend.payload.JoinResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository repo;
    @Mock
    private CourseProgressRepository progressRepo;
    @Mock
    private JitsiTokenService tokenService;

    @InjectMocks
    private SessionService service;

    @Test
    void upcomingThrowsWhenNotEnrolled() {
        when(progressRepo.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(false);

        assertThatThrownBy(() -> service.upcoming("course-1", "user-1", false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void joinReturnsJwtWhenModerator() {
        Session session = new Session();
        session.setId("session-1");
        session.setCourseId("course-1");
        session.setRoomName("room-1");
        session.setStartTime(Instant.now());
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(tokenService.mintUserToken(any(User.class), any(Session.class), eq(true))).thenReturn("jwt");

        User user = new User();
        user.setId("user-1");
        user.setName("User");

        JoinResponse response = service.join("session-1", user, true);

        assertThat(response.getRoomName()).isEqualTo("room-1");
        assertThat(response.getJwt()).isEqualTo("jwt");
        assertThat(response.getDisplayName()).isEqualTo("User");
    }

    @Test
    void recordEventStatsMergesWatchSeconds() {
        Session session = new Session();
        session.setId("session-1");
        session.setCourseId("course-1");
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.STATS);
        dto.setSecondsWatched(15L);

        service.recordEvent("session-1", "user-1", dto);

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(repo).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getWatchSecondsByUserId().get("user-1")).isEqualTo(15L);
    }
}
