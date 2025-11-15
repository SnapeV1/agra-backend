package org.agra.agra_backend.controller;

import jakarta.validation.Valid;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.TicketThreadResponse;
import org.agra.agra_backend.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketThreadResponse> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                                             Authentication authentication) {
        User user = requireUser(authentication);
        TicketThreadResponse response = ticketService.createTicket(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public List<Ticket> getMyTickets(Authentication authentication) {
        User user = requireUser(authentication);
        return ticketService.getTicketsForUser(user);
    }

    @GetMapping
    public List<Ticket> getAllTickets(Authentication authentication) {
        User user = requireUser(authentication);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access only");
        }
        return ticketService.getTickets(user);
    }

    @GetMapping("/{ticketId}")
    public TicketThreadResponse getTicket(@PathVariable String ticketId, Authentication authentication) {
        User user = requireUser(authentication);
        return ticketService.getTicketThread(user, ticketId);
    }

    @PostMapping("/{ticketId}/message")
    public TicketMessage sendMessage(@PathVariable String ticketId,
                                     @Valid @RequestBody SendTicketMessageRequest request,
                                     Authentication authentication) {
        User user = requireUser(authentication);
        return ticketService.sendMessage(user, ticketId, request);
    }

    @PatchMapping("/{ticketId}/close")
    public Ticket closeTicket(@PathVariable String ticketId, Authentication authentication) {
        User user = requireUser(authentication);
        return ticketService.closeTicket(user, ticketId);
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }
}

