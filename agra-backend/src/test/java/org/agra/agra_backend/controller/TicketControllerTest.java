package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.TicketThreadResponse;
import org.agra.agra_backend.payload.UpdateTicketStatusRequest;
import org.agra.agra_backend.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController controller;

    @Test
    void createTicketRejectsMissingAuth() {
        CreateTicketRequest request = new CreateTicketRequest();

        assertThatThrownBy(() -> controller.createTicket(request, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createTicketReturnsCreated() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Help");
        request.setMessage("Details");

        TicketThreadResponse threadResponse = new TicketThreadResponse(new Ticket(), List.of());
        when(ticketService.createTicket(user, request)).thenReturn(threadResponse);

        ResponseEntity<TicketThreadResponse> response = controller.createTicket(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(threadResponse);
    }

    @Test
    void getAllTicketsRejectsNonAdmin() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        user.setRole("USER");
        when(authentication.getPrincipal()).thenReturn(user);

        assertThatThrownBy(() -> controller.getAllTickets(authentication))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sendMessageReturnsServiceResult() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("Reply");

        TicketMessage message = new TicketMessage();
        when(ticketService.sendMessage(user, "ticket-1", request)).thenReturn(message);

        TicketMessage response = controller.sendMessage("ticket-1", request, authentication);

        assertThat(response).isSameAs(message);
    }

    @Test
    void updateTicketStatusReturnsServiceResult() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("admin-1");
        user.setRole("ADMIN");
        when(authentication.getPrincipal()).thenReturn(user);

        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.setStatus("OPEN");

        Ticket ticket = new Ticket();
        when(ticketService.updateTicketStatus(user, "ticket-1", "OPEN")).thenReturn(ticket);

        Ticket response = controller.updateTicketStatus("ticket-1", request, authentication);

        assertThat(response).isSameAs(ticket);
    }
}
