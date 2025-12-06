package org.agra.agra_backend.service;

import lombok.RequiredArgsConstructor;
import org.agra.agra_backend.Misc.JitsiTokenService;
import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.SessionRepository;
import org.agra.agra_backend.model.Session;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.AttendanceEventDto;
import org.agra.agra_backend.payload.CreateSessionDto;
import org.agra.agra_backend.payload.JoinResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository repo;
    private final CourseProgressRepository progressRepo;
    private final JitsiTokenService tokenService;

    public Session create(String courseId, CreateSessionDto dto) {
        Session s = new Session();
        s.setCourseId(courseId);
        s.setTitle(dto.getTitle());
        s.setDescription(dto.getDescription());
        s.setStartTime(dto.getStartTime());
        s.setEndTime(dto.getEndTime());
        s.setLobbyEnabled(Boolean.TRUE.equals(dto.getLobbyEnabled()));
        s.setRecordingEnabled(Boolean.TRUE.equals(dto.getRecordingEnabled()));
        s.setRoomName(generateRoomName(courseId));
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    public List<Session> upcoming(String courseId, String userId, boolean moderator) {
        enforceCourseAccess(courseId, userId, moderator);
        return repo.findByCourseIdAndStartTimeAfterOrderByStartTimeAsc(courseId, Instant.now().minus(Duration.ofMinutes(15)));
    }

    public JoinResponse join(String sessionId, User user, boolean moderator) {
        System.out.println("[JITSI][Service] Join called. sessionId=" + sessionId + ", userId=" + user.getId() + ", moderator=" + moderator);
        Session s = repo.findById(sessionId).orElseThrow(() -> {
            System.out.println("[JITSI][Service] Session not found for id=" + sessionId);
            return notFound("Session");
        });
        System.out.println("[JITSI][Service] Loaded session. courseId=" + s.getCourseId() + ", room=" + s.getRoomName() + ", start=" + s.getStartTime() + ", end=" + s.getEndTime());
        enforceCourseAccess(s.getCourseId(), user.getId(), moderator);
        System.out.println("[JITSI][Service] Access check passed. Generating token...");
        String jwt = tokenService.mintUserToken(user, s, moderator);
        System.out.println("[JITSI][Service] Token generated? " + (jwt != null) + " tokenLength=" + (jwt == null ? 0 : jwt.length()));

        return new JoinResponse(s.getRoomName(), /*domain*/ null, jwt, user.getName(), user.getPicture());

    }

    private void enforceCourseAccess(String courseId, String userId, boolean moderator) {
        System.out.println("[JITSI][Service] enforceCourseAccess courseId=" + courseId + ", userId=" + userId + ", moderator=" + moderator);
        if (moderator) return;
        boolean enrolled = progressRepo.existsByUserIdAndCourseId(userId, courseId);
        System.out.println("[JITSI][Service] Enrollment check result=" + enrolled);
        if (!enrolled) throw forbidden("Not enrolled in this course");
    }

    private static String generateRoomName(String courseId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "course_" + courseId + "_" + suffix;
    }

    private static ResponseStatusException notFound(String what) { return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found"); }
    private static ResponseStatusException forbidden(String why) { return new ResponseStatusException(HttpStatus.FORBIDDEN, why); }
    public Session getById(String sessionId) {
        return repo.findById(sessionId)
                .orElseThrow(() -> notFound("Session"));
    }


    public void recordEvent(String sessionId, String userId, AttendanceEventDto dto) {
        Session s = repo.findById(sessionId).orElseThrow(() -> notFound("Session"));

        // Initialize collections if null
        if (s.getAttendeeIds() == null) s.setAttendeeIds(new ArrayList<>());
        if (s.getWatchSecondsByUserId() == null) s.setWatchSecondsByUserId(new HashMap<>());

        switch (Objects.requireNonNull(dto.getType(), "Event type is required")) {
            case JOIN -> {
                if (!s.getAttendeeIds().contains(userId)) {
                    s.getAttendeeIds().add(userId);
                }
            }
            case STATS -> {
                long delta = Math.max(0L, dto.getSecondsWatched() == null ? 0L : dto.getSecondsWatched());
                s.getWatchSecondsByUserId().merge(userId, delta, Long::sum);
            }
            case LEAVE -> {
                // Optional: keep attendees list as a historical snapshot (don’t remove).
                // If you prefer to remove on leave, uncomment the next line:
                // s.getAttendeeIds().remove(userId);

                long delta = Math.max(0L, dto.getSecondsWatched() == null ? 0L : dto.getSecondsWatched());
                s.getWatchSecondsByUserId().merge(userId, delta, Long::sum);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported event type");
        }

        s.setUpdatedAt(Instant.now());
        repo.save(s);
    }
}

