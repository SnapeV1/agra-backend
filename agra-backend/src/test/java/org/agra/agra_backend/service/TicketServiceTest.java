package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.TicketMessageRepository;
import org.agra.agra_backend.dao.TicketRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
