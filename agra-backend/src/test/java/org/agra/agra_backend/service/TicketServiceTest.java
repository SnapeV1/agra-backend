package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.TicketMessageRepository;
import org.agra.agra_backend.dao.TicketRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketStatus;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMessageRepository ticketMessageRepository;
    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private TicketService service;

    @Test
    void createTicketRejectsNullRequester() {
        CreateTicketRequest request = new CreateTicketRequest();

        assertThatThrownBy(() -> service.createTicket(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(ticketRepository, ticketMessageRepository, messagingTemplate,
                userRepository, notificationRepository, notificationService, cloudinaryService);
    }

    @Test
    void sendMessageRejectsNullActor() {
        SendTicketMessageRequest request = new SendTicketMessageRequest();

        assertThatThrownBy(() -> service.sendMessage(null, "ticket-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(ticketRepository, ticketMessageRepository, messagingTemplate,
                userRepository, notificationRepository, notificationService, cloudinaryService);
    }

    @Test
    void updateTicketStatusRejectsNullActor() {
        assertThatThrownBy(() -> service.updateTicketStatus(null, "ticket-1", "OPEN"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(ticketRepository, ticketMessageRepository, messagingTemplate,
                userRepository, notificationRepository, notificationService, cloudinaryService);
    }

    @Test
    void createTicketRejectsBlankSubject() {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("  ");
        request.setMessage("Help");

        assertThatThrownBy(() -> service.createTicket(requester, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createTicketRejectsMissingContentAndAttachment() {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Need help");
        request.setMessage(" ");
        request.setAttachmentUrl(" ");

        assertThatThrownBy(() -> service.createTicket(requester, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sendMessageRejectsMissingContentAndAttachment() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent(" ");
        request.setAttachmentUrl(" ");

        assertThatThrownBy(() -> service.sendMessage(actor, "ticket-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateTicketStatusRejectsInvalidStatus() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicketStatus(actor, "ticket-1", "not-a-status"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
