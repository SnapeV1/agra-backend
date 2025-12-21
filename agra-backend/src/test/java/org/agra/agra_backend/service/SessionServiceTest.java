package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.SessionRepository;
import org.agra.agra_backend.misc.JitsiTokenService;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.AttendanceEventDto;
import org.agra.agra_backend.payload.CreateSessionDto;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    void createStoresSessionFields() {
        CreateSessionDto dto = new CreateSessionDto();
        dto.setTitle("Session");
        dto.setDescription("Desc");
        dto.setStartTime(Instant.now());
        dto.setEndTime(Instant.now().plusSeconds(3600));
        dto.setLobbyEnabled(true);
        dto.setRecordingEnabled(false);

        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session created = service.create("course-1", dto);

        assertThat(created.getCourseId()).isEqualTo("course-1");
        assertThat(created.getTitle()).isEqualTo("Session");
        assertThat(created.getRoomName()).contains("course-");
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void upcomingReturnsSessionsForModerator() {
        when(repo.findByCourseIdAndStartTimeAfterOrderByStartTimeAsc(eq("course-1"), any()))
                .thenReturn(List.of(new Session()));

        List<Session> sessions = service.upcoming("course-1", "user-1", true);

        assertThat(sessions).hasSize(1);
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
    void joinThrowsWhenSessionMissing() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        User user = new User();
        user.setId("user-1");

        assertThatThrownBy(() -> service.join("missing", user, false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void joinThrowsWhenNotEnrolled() {
        Session session = new Session();
        session.setId("session-1");
        session.setCourseId("course-1");
        session.setRoomName("room-1");
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(progressRepo.existsByUserIdAndCourseId("user-1", "course-1")).thenReturn(false);

        User user = new User();
        user.setId("user-1");

        assertThatThrownBy(() -> service.join("session-1", user, false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getByIdReturnsSession() {
        Session session = new Session();
        session.setId("session-1");
        when(repo.findById("session-1")).thenReturn(Optional.of(session));

        Session result = service.getById("session-1");

        assertThat(result.getId()).isEqualTo("session-1");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
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

    @Test
    void recordEventJoinAddsAttendee() {
        Session session = new Session();
        session.setId("session-1");
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.JOIN);

        service.recordEvent("session-1", "user-1", dto);

        assertThat(session.getAttendeeIds()).contains("user-1");
    }

    @Test
    void recordEventJoinDoesNotDuplicateAttendee() {
        Session session = new Session();
        session.setId("session-1");
        session.setAttendeeIds(new ArrayList<>(List.of("user-1")));
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.JOIN);

        service.recordEvent("session-1", "user-1", dto);

        assertThat(session.getAttendeeIds()).containsExactly("user-1");
    }

    @Test
    void recordEventLeaveMergesSeconds() {
        Session session = new Session();
        session.setId("session-1");
        session.setAttendeeIds(List.of("user-1"));
        session.setWatchSecondsByUserId(new HashMap<>(Map.of("user-1", 5L)));
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.LEAVE);
        dto.setSecondsWatched(10L);

        service.recordEvent("session-1", "user-1", dto);

        assertThat(session.getWatchSecondsByUserId().get("user-1")).isEqualTo(15L);
    }

    @Test
    void recordEventStatsDefaultsNullSecondsToZero() {
        Session session = new Session();
        session.setId("session-1");
        session.setWatchSecondsByUserId(new HashMap<>());
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.STATS);
        dto.setSecondsWatched(null);

        service.recordEvent("session-1", "user-1", dto);

        assertThat(session.getWatchSecondsByUserId()).containsEntry("user-1", 0L);
    }

    @Test
    void recordEventInitializesCollectionsWhenNull() {
        Session session = new Session();
        session.setId("session-1");
        when(repo.findById("session-1")).thenReturn(Optional.of(session));
        when(repo.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.LEAVE);
        dto.setSecondsWatched(4L);

        service.recordEvent("session-1", "user-1", dto);

        assertThat(session.getAttendeeIds()).isNotNull();
        assertThat(session.getWatchSecondsByUserId()).containsEntry("user-1", 4L);
    }

    @Test
    void recordEventRejectsNullType() {
        Session session = new Session();
        when(repo.findById("session-1")).thenReturn(Optional.of(session));

        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(null);

        assertThatThrownBy(() -> service.recordEvent("session-1", "user-1", dto))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Event type is required");
    }
}
