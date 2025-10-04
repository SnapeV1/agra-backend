package org.agra.agra_backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jitsi")
@CrossOrigin(origins = "*")
public class ChatController {

    @GetMapping("/create-room")
    public Map<String, String> createRoom() {
        String roomName = "room-" + UUID.randomUUID();
        return Map.of("roomName", roomName);
    }
}
