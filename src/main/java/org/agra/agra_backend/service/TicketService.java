package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.TicketMessageRepository;
import org.agra.agra_backend.dao.TicketRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.TicketStatus;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.TicketEventPayload;
import org.agra.agra_backend.payload.TicketThreadResponse;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         TicketMessageRepository ticketMessageRepository,
                         SimpMessagingTemplate messagingTemplate,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public TicketThreadResponse createTicket(User requester, CreateTicketRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setUserInfo(toUserInfo(requester));
        ticket.setSubject(request.getSubject().trim());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        Ticket savedTicket = ticketRepository.save(ticket);

        TicketMessage initialMessage = new TicketMessage();
        initialMessage.setTicketId(savedTicket.getId());
        initialMessage.setSenderId(requester.getId());
        initialMessage.setContent(request.getMessage().trim());
        initialMessage.setRecipientId(null);
        initialMessage.setSender(savedTicket.getUserInfo());
        initialMessage.setRecipient(null);
        initialMessage.setTimestamp(now);
        initialMessage.setAdminMessage(false);

        TicketMessage savedMessage = ticketMessageRepository.save(initialMessage);
        publishMessageEvent(savedTicket, savedMessage);

        return new TicketThreadResponse(savedTicket, List.of(savedMessage));
    }

    public List<Ticket> getTickets(User actor) {
        if (isAdmin(actor)) {
            List<Ticket> tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
            enrichTickets(tickets);
            return tickets;
        }
        List<Ticket> tickets = ticketRepository.findByUserInfo_IdOrderByCreatedAtDesc(actor.getId());
        enrichTickets(tickets);
        return tickets;
    }

    public List<Ticket> getTicketsForUser(User actor) {
        List<Ticket> tickets = ticketRepository.findByUserInfo_IdOrderByCreatedAtDesc(actor.getId());
        enrichTickets(tickets);
        return tickets;
    }

    public TicketThreadResponse getTicketThread(User actor, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        ensureCanView(actor, ticket);
        enrichTicket(ticket);
        List<TicketMessage> messages = ticketMessageRepository.findByTicketIdOrderByTimestampAsc(ticket.getId());
        messages.forEach(this::enrichMessage);
        return new TicketThreadResponse(ticket, messages);
    }

    public TicketMessage sendMessage(User actor, String ticketId, SendTicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        ensureCanView(actor, ticket);

        if (!isAdmin(actor) && ticket.getStatus() != TicketStatus.OPEN) {
            throw new ResponseStatusException(FORBIDDEN, "Ticket is not open for replies");
        }

        if (isAdmin(actor) && ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot reply to closed ticket");
        }

        LocalDateTime now = LocalDateTime.now();
        TicketMessage message = new TicketMessage();
        message.setTicketId(ticket.getId());
        message.setSenderId(actor.getId());
        message.setContent(request.getContent().trim());
        message.setTimestamp(now);
        boolean isAdmin = isAdmin(actor);
        message.setAdminMessage(isAdmin);

        UserInfo senderInfo = toUserInfo(actor);
        message.setSender(senderInfo);

        UserInfo recipientInfo = isAdmin ? ticket.getUserInfo() : ticket.getAdminInfo();
        String recipientId = resolveUserId(recipientInfo);
        message.setRecipientId(recipientId);
        message.setRecipient(recipientInfo);

        TicketMessage savedMessage = ticketMessageRepository.save(message);

        ticket.setUpdatedAt(now);
        String currentAdminId = resolveUserId(ticket.getAdminInfo());
        boolean wasAssignedToDifferentAdmin = isAdmin && (currentAdminId == null || !Objects.equals(currentAdminId, actor.getId()));
        if (wasAssignedToDifferentAdmin) {
            ticket.setAdminInfo(toUserInfo(actor));
        }
        Ticket savedTicket = ticketRepository.save(ticket);

        publishMessageEvent(savedTicket, savedMessage);
        if (wasAssignedToDifferentAdmin) {
            publishAssignmentEvent(savedTicket);
        }
        return savedMessage;
    }

    public Ticket closeTicket(User actor, String ticketId) {
        requireAdmin(actor);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setUpdatedAt(LocalDateTime.now());
        enrichTicket(ticket);
        Ticket saved = ticketRepository.save(ticket);
        publishLifecycleEvent("CLOSED", saved);
        return saved;
    }

    private void ensureCanView(User actor, Ticket ticket) {
        if (isAdmin(actor)) {
            return;
        }
        if (!Objects.equals(resolveUserId(ticket.getUserInfo()), actor.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You cannot view this ticket");
        }
    }

    private void requireAdmin(User actor) {
        if (!isAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Admin access required");
        }
    }

    private boolean isAdmin(User actor) {
        return actor != null && "ADMIN".equalsIgnoreCase(actor.getRole());
    }

    private void publishMessageEvent(Ticket ticket, TicketMessage message) {
        TicketEventPayload payload = new TicketEventPayload("MESSAGE", ticket.getId(), ticket, message);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticket.getId(), payload);

        if (message.isAdminMessage()) {
            notifyUser(ticket.getUserInfo(), payload);
        } else if (ticket.getAdminInfo() != null) {
            notifyUser(ticket.getAdminInfo(), payload);
        }
    }

    private void publishAssignmentEvent(Ticket ticket) {
        TicketEventPayload payload = new TicketEventPayload("ASSIGNED", ticket.getId(), ticket, null);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticket.getId(), payload);
        if (ticket.getUserInfo() != null) {
            notifyUser(ticket.getUserInfo(), payload);
        }
        if (ticket.getAdminInfo() != null) {
            notifyUser(ticket.getAdminInfo(), payload);
        }
    }

    private void publishLifecycleEvent(String type, Ticket ticket) {
        TicketEventPayload payload = new TicketEventPayload(type, ticket.getId(), ticket, null);
        messagingTemplate.convertAndSend("/topic/tickets/" + ticket.getId(), payload);
        if (ticket.getUserInfo() != null) {
            notifyUser(ticket.getUserInfo(), payload);
        }
        if (ticket.getAdminInfo() != null) {
            notifyUser(ticket.getAdminInfo(), payload);
        }
    }

    private void notifyUser(UserInfo info, TicketEventPayload payload) {
        String userId = resolveUserId(info);
        if (userId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/queue/user/" + userId + "/ticket-notifications", payload);
    }

    private void enrichTickets(List<Ticket> tickets) {
        if (tickets == null) return;
        tickets.forEach(this::enrichTicket);
    }

    private void enrichTicket(Ticket ticket) {
        if (ticket == null) {
            return;
        }
        if (ticket.getUserInfo() == null || ticket.getUserInfo().getName() == null) {
            ticket.setUserInfo(loadUserInfo(resolveUserId(ticket.getUserInfo())));
        }
        if (ticket.getAdminInfo() != null && ticket.getAdminInfo().getName() == null) {
            ticket.setAdminInfo(loadUserInfo(resolveUserId(ticket.getAdminInfo())));
        }
    }

    private void enrichMessage(TicketMessage message) {
        if (message == null) {
            return;
        }
        if ((message.getSender() == null || message.getSender().getName() == null) && message.getSenderId() != null) {
            message.setSender(loadUserInfo(message.getSenderId()));
        }
        if ((message.getRecipient() == null || message.getRecipient().getName() == null) && message.getRecipientId() != null) {
            message.setRecipient(loadUserInfo(message.getRecipientId()));
        }
    }

    private UserInfo loadUserInfo(String userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(this::toUserInfo)
                .orElse(null);
    }

    private UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setName(user.getName());
        info.setEmail(user.getEmail());
        info.setPicture(user.getPicture());
        return info;
    }

    private String resolveUserId(UserInfo info) {
        return info != null ? info.getId() : null;
    }
}
