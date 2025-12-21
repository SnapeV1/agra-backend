package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.TicketMessageRepository;
import org.agra.agra_backend.dao.TicketRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.TicketStatus;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.TicketThreadResponse;
import org.agra.agra_backend.payload.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMessageRepository ticketMessageRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
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

    @Test
    void updateTicketStatusRejectsMissingStatus() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicketStatus(actor, "ticket-1", " "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createTicketWithAttachmentUploadsAndNotifies() throws Exception {
        User requester = new User();
        requester.setId("user-1");
        requester.setName("User");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject(" Need help ");
        request.setMessage(" Hello ");

        MockMultipartFile attachment = new MockMultipartFile("file", "img.png", "image/png", "data".getBytes());

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cloudinaryService.uploadTicketAttachment(any(), anyString(), anyString()))
                .thenReturn(Map.of("secure_url", "http://file"));

        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");
        when(userRepository.findAll()).thenReturn(List.of(admin));

        TicketThreadResponse response = service.createTicket(requester, request, attachment);

        assertThat(response.getTicket().getId()).isEqualTo("ticket-1");
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationService).createStatusForUser(eq("admin-1"), any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("admin-1"), eq("/queue/notifications"), any(Notification.class));
    }

    @Test
    void createTicketUsesProvidedAttachmentUrlWithoutUpload() {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Help");
        request.setMessage("Message");
        request.setAttachmentUrl(" http://attachment ");

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());

        TicketThreadResponse response = service.createTicket(requester, request, null);

        assertThat(response.getTicket().getAttachmentUrl()).isEqualTo("http://attachment");
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void createTicketAttachmentRequiresTicketId() {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Help");
        request.setMessage("Message");

        MockMultipartFile attachment = new MockMultipartFile("file", "img.png", "image/png", "data".getBytes());

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.createTicket(requester, request, attachment))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void createTicketAttachmentRequiresSecureUrl() throws Exception {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Help");
        request.setMessage("Message");

        MockMultipartFile attachment = new MockMultipartFile("file", "img.png", "image/png", "data".getBytes());

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(cloudinaryService.uploadTicketAttachment(any(), anyString(), anyString()))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> service.createTicket(requester, request, attachment))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void createTicketAttachmentHandlesIOException() throws Exception {
        User requester = new User();
        requester.setId("user-1");

        CreateTicketRequest request = new CreateTicketRequest();
        request.setSubject("Help");
        request.setMessage("Message");

        MockMultipartFile attachment = new MockMultipartFile("file", "img.png", "image/png", "data".getBytes());

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(cloudinaryService.uploadTicketAttachment(any(), anyString(), anyString()))
                .thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.createTicket(requester, request, attachment))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getTicketsAdminEnrichesTickets() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setUserInfo(null);
        UserInfo adminInfo = new UserInfo();
        adminInfo.setId("admin-1");
        ticket.setAdminInfo(adminInfo);
        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(ticket));

        User loaded = new User();
        loaded.setId("admin-1");
        loaded.setName("Admin");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(loaded));

        List<Ticket> result = service.getTickets(admin);

        assertThat(result.get(0).getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(result.get(0).getAdminInfo().getName()).isEqualTo("Admin");
    }

    @Test
    void getTicketsForUserUsesRepository() {
        User actor = new User();
        actor.setId("user-1");

        when(ticketRepository.findByUserInfo_IdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(new Ticket()));

        List<Ticket> result = service.getTicketsForUser(actor);

        assertThat(result).hasSize(1);
    }

    @Test
    void getTicketThreadRejectsForbidden() {
        User actor = new User();
        actor.setId("user-1");

        Ticket ticket = new Ticket();
        UserInfo userInfo = new UserInfo();
        userInfo.setId("other");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getTicketThread(actor, "ticket-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getTicketThreadEnrichesMessages() {
        User actor = new User();
        actor.setId("user-1");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        UserInfo adminInfo = new UserInfo();
        adminInfo.setId("admin-1");
        ticket.setAdminInfo(adminInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        TicketMessage message = new TicketMessage();
        message.setTicketId("ticket-1");
        message.setSenderId("user-1");
        message.setRecipientId("admin-1");
        when(ticketMessageRepository.findByTicketIdOrderByTimestampAsc("ticket-1"))
                .thenReturn(List.of(message));

        User loadedUser = new User();
        loadedUser.setId("user-1");
        loadedUser.setName("User");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(loadedUser));

        User loadedAdmin = new User();
        loadedAdmin.setId("admin-1");
        loadedAdmin.setName("Admin");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(loadedAdmin));

        TicketThreadResponse response = service.getTicketThread(actor, "ticket-1");

        assertThat(response.getMessages().get(0).getSender()).isNotNull();
        assertThat(response.getMessages().get(0).getRecipient()).isNotNull();
    }

    @Test
    void sendMessageRejectsClosedTicketForUser() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.RESOLVED);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("Reply");

        assertThatThrownBy(() -> service.sendMessage(actor, "ticket-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sendMessageRejectsResolvedTicketForAdmin() {
        User actor = new User();
        actor.setId("admin-1");
        actor.setRole("ADMIN");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setUserInfo(new UserInfo());
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("Reply");

        assertThatThrownBy(() -> service.sendMessage(actor, "ticket-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sendMessageAdminAssignsTicketAndSendsNotification() {
        User actor = new User();
        actor.setId("admin-1");
        actor.setRole("ADMIN");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setSubject("Subject");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("a".repeat(130));

        TicketMessage result = service.sendMessage(actor, "ticket-1", request);

        assertThat(result.isAdminMessage()).isTrue();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getContent()).contains("Support replied to your support ticket");
        assertThat(notificationCaptor.getValue().getContent()).contains("...");
        verify(notificationService).createStatusForUser(eq("user-1"), any(Notification.class));
    }

    @Test
    void sendMessageAdminSkipsNotifyWhenUserIdMissing() {
        User actor = new User();
        actor.setId("admin-1");
        actor.setRole("ADMIN");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(null);
        ticket.setUserInfo(userInfo);
        UserInfo adminInfo = new UserInfo();
        adminInfo.setId("admin-1");
        ticket.setAdminInfo(adminInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("Reply");

        service.sendMessage(actor, "ticket-1", request);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendMessageWithAttachmentUrlOnly() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");
        actor.setName("User");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent(" ");
        request.setAttachmentUrl(" http://attachment ");

        TicketMessage message = service.sendMessage(actor, "ticket-1", request);

        assertThat(message.getAttachmentUrl()).isEqualTo("http://attachment");
    }

    @Test
    void sendMessageUserNotifiesAssignedAdmin() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");
        actor.setName("User");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        UserInfo adminInfo = new UserInfo();
        adminInfo.setId("admin-1");
        ticket.setAdminInfo(adminInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendTicketMessageRequest request = new SendTicketMessageRequest();
        request.setContent("Reply");

        service.sendMessage(actor, "ticket-1", request);

        verify(notificationService).createStatusForUser(eq("admin-1"), any(Notification.class));
    }

    @Test
    void updateTicketStatusReturnsExistingWhenUnchanged() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));

        Ticket result = service.updateTicketStatus(actor, "ticket-1", "OPEN");

        assertThat(result).isSameAs(ticket);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void updateTicketStatusAcceptsClosedAsResolved() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        ticket.setStatus(TicketStatus.OPEN);
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = service.updateTicketStatus(actor, "ticket-1", "CLOSED");

        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void closeTicketRequiresAdmin() {
        User actor = new User();
        actor.setId("user-1");
        actor.setRole("USER");

        assertThatThrownBy(() -> service.closeTicket(actor, "ticket-1"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void closeTicketUpdatesStatusAndPublishes() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setRole("ADMIN");

        Ticket ticket = new Ticket();
        ticket.setId("ticket-1");
        UserInfo userInfo = new UserInfo();
        userInfo.setId("user-1");
        ticket.setUserInfo(userInfo);
        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = service.closeTicket(admin, "ticket-1");

        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }
}
