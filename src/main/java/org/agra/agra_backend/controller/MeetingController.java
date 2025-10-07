package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.MeetingRoomRepository;
import org.agra.agra_backend.model.MeetingRoom;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.JitsiTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/jitsi")
@CrossOrigin(origins = "*")
public class MeetingController {

    private final JitsiTokenService jitsiTokenService;
    private final MeetingRoomRepository meetingRoomRepository;
    private static final Logger log = LoggerFactory.getLogger(MeetingController.class);

    public MeetingController(JitsiTokenService jitsiTokenService, MeetingRoomRepository meetingRoomRepository) {
        this.jitsiTokenService = jitsiTokenService;
        this.meetingRoomRepository = meetingRoomRepository;
    }

    @PostMapping("/meetings")
    public ResponseEntity<?> createMeeting(Authentication authentication, @RequestParam(required = false) String room) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.debug("Create meeting requested without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        String creatorId = user.getId();
        String username = user.getName();

        String roomName = (room == null || room.isBlank()) ? ("room-" + UUID.randomUUID()) : room;

        Optional<MeetingRoom> existing = meetingRoomRepository.findById(roomName);
        MeetingRoom meetingRoom = existing.orElseGet(() -> new MeetingRoom(roomName, creatorId));
        meetingRoomRepository.save(meetingRoom);

        boolean isCreator = creatorId.equals(meetingRoom.getCreatedBy());
        String token = jitsiTokenService.generateToken(roomName, username, isCreator);

        log.info("Meeting created: room={}, creatorId={}, moderator={}", roomName, creatorId, isCreator);

        return ResponseEntity.ok(Map.of(
                "room", roomName,
                "moderator", isCreator,
                "jwt", token
        ));
    }

    @PostMapping("/meetings/{room}/token")
    public ResponseEntity<?> getMeetingToken(@PathVariable String room, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.debug("Get token requested without authentication for room={}", room);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        String username = user.getName();

        Optional<MeetingRoom> existing = meetingRoomRepository.findById(room);
        MeetingRoom meetingRoom = existing.orElseGet(() -> new MeetingRoom(room, userId));
        meetingRoomRepository.save(meetingRoom);

        boolean isCreator = userId.equals(meetingRoom.getCreatedBy());
        String token = jitsiTokenService.generateToken(room, username, isCreator);

        log.info("Token issued: room={}, userId={}, moderator={}", room, userId, isCreator);

        return ResponseEntity.ok(Map.of(
                "room", room,
                "moderator", isCreator,
                "jwt", token
        ));
    }
}