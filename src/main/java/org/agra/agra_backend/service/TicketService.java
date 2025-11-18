package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.dao.TicketMessageRepository;
import org.agra.agra_backend.dao.TicketRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.Notification;
import org.agra.agra_backend.model.NotificationType;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.TicketStatus;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.CreateTicketRequest;
import org.agra.agra_backend.payload.SendTicketMessageRequest;
import org.agra.agra_backend.payload.TicketEventPayload;
import org.agra.agra_backend.payload.TicketThreadResponse;
import org.agra.agra_backend.payload.UserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    public TicketService(TicketRepository ticketRepository,
                         TicketMessageRepository ticketMessageRepository,
                         SimpMessagingTemplate messagingTemplate,
                         UserRepository userRepository,
                         NotificationRepository notificationRepository,
                         NotificationService notificationService,
                         CloudinaryService cloudinaryService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.cloudinaryService = cloudinaryService;
    }

    public TicketThreadResponse createTicket(User requester, CreateTicketRequest request) {
        return createTicket(requester, request, null);
    }

    public TicketThreadResponse createTicket(User requester, CreateTicketRequest request, MultipartFile attachment) {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setUserInfo(toUserInfo(requester));
        String normalizedSubject = request.getSubject() != null ? request.getSubject().trim() : null;
        if (normalizedSubject == null || normalizedSubject.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is required");
        }
        String normalizedMessage = normalizeContent(request.getMessage());
        String providedAttachmentUrl = normalizeAttachmentUrl(request.getAttachmentUrl());
        if (normalizedMessage == null && providedAttachmentUrl == null && (attachment == null || attachment.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content or attachment is required");
        }

        ticket.setSubject(normalizedSubject);
        ticket.setAttachmentUrl(providedAttachmentUrl);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        Ticket savedTicket = ticketRepository.save(ticket);

        String attachmentUrl = resolveAttachment(savedTicket, attachment);
        if (attachmentUrl != null && !attachmentUrl.equals(ticket.getAttachmentUrl())) {
            savedTicket.setAttachmentUrl(attachmentUrl);
            savedTicket = ticketRepository.save(savedTicket);
        }

        TicketMessage initialMessage = new TicketMessage();
        initialMessage.setTicketId(savedTicket.getId());
        initialMessage.setSenderId(requester.getId());
        initialMessage.setContent(normalizedMessage);
        initialMessage.setRecipientId(null);
        initialMessage.setSender(savedTicket.getUserInfo());
        initialMessage.setRecipient(null);
        initialMessage.setTimestamp(now);
        initialMessage.setAdminMessage(false);
        initialMessage.setAttachmentUrl(attachmentUrl != null ? attachmentUrl : providedAttachmentUrl);

        TicketMessage savedMessage = ticketMessageRepository.save(initialMessage);
        publishMessageEvent(savedTicket, savedMessage);
        emitNotificationForMessage(savedTicket, savedMessage);

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
        return sendMessage(actor, ticketId, request, null);
    }

    public TicketMessage sendMessage(User actor, String ticketId, SendTicketMessageRequest request, MultipartFile attachment) {
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
        String normalizedContent = normalizeContent(request.getContent());
        String providedAttachmentUrl = normalizeAttachmentUrl(request.getAttachmentUrl());
        if (normalizedContent == null && providedAttachmentUrl == null && (attachment == null || attachment.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content or attachment is required");
        }

        TicketMessage message = new TicketMessage();
        message.setTicketId(ticket.getId());
        message.setSenderId(actor.getId());
        message.setContent(normalizedContent);
        message.setTimestamp(now);
        String attachmentUrl = providedAttachmentUrl;
        String uploadedUrl = resolveAttachment(ticket, attachment);
        if (uploadedUrl != null) {
            attachmentUrl = uploadedUrl;
        }
        message.setAttachmentUrl(attachmentUrl);
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
        emitNotificationForMessage(savedTicket, savedMessage);
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

    public Ticket updateTicketStatus(User actor, String ticketId, String statusValue) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        ensureCanView(actor, ticket);

        TicketStatus newStatus = parseStatus(statusValue);
        if (ticket.getStatus() == newStatus) {
            return ticket;
        }
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        publishLifecycleEvent(newStatus.name(), saved);
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
        messagingTemplate.convertAndSendToUser(userId, "/queue/ticket-notifications", payload);
    }

    private void emitNotificationForMessage(Ticket ticket, TicketMessage message) {
        if (ticket == null || message == null) {
            return;
        }
        List<String> recipients = resolveNotificationRecipients(ticket, message);
        if (recipients.isEmpty()) {
            return;
        }

        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                buildNotificationContent(ticket, message),
                NotificationType.TICKET,
                LocalDateTime.now()
        );
        notificationRepository.save(notification);

        recipients.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(recipientId -> {
                    notificationService.createStatusForUser(recipientId, notification);
                    messagingTemplate.convertAndSendToUser(recipientId, "/queue/notifications", notification);
                });
    }

    private List<String> resolveNotificationRecipients(Ticket ticket, TicketMessage message) {
        if (message.isAdminMessage()) {
            String userId = resolveUserId(ticket.getUserInfo());
            return userId == null ? java.util.List.of() : java.util.List.of(userId);
        }

        String adminId = resolveUserId(ticket.getAdminInfo());
        if (adminId != null) {
            return java.util.List.of(adminId);
        }

        return userRepository.findAll().stream()
                .filter(this::isAdmin)
                .map(User::getId)
                .filter(id -> id != null && !Objects.equals(id, message.getSenderId()))
                .toList();
    }

    private String buildNotificationContent(Ticket ticket, TicketMessage message) {
        String senderName = null;
        if (message.getSender() != null && message.getSender().getName() != null) {
            senderName = message.getSender().getName();
        }
        if (senderName == null || senderName.isBlank()) {
            senderName = message.isAdminMessage() ? "Support" : "User";
        }

        String subject = ticket.getSubject();
        StringBuilder builder = new StringBuilder();
        builder.append(senderName)
                .append(message.isAdminMessage() ? " replied to your support ticket" : " sent a new support message");
        if (subject != null && !subject.isBlank()) {
            builder.append(" (").append(subject.trim()).append(")");
        }

        String snippet = message.getContent();
        if (snippet != null) {
            snippet = snippet.trim();
            if (!snippet.isEmpty()) {
                if (snippet.length() > 120) {
                    snippet = snippet.substring(0, 117) + "...";
                }
                builder.append(": ").append(snippet);
            }
        }
        return builder.toString();
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

    private String normalizeAttachmentUrl(String attachmentUrl) {
        if (attachmentUrl == null) {
            return null;
        }
        String trimmed = attachmentUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TicketStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        try {
            return TicketStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket status: " + statusValue);
        }
    }

    private String resolveAttachment(Ticket ticket, MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return null;
        }
        if (ticket == null || ticket.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ticket must exist before uploading attachments");
        }

        String ownerId = resolveUserId(ticket.getUserInfo());
        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadTicketAttachment(
                    attachment,
                    ownerId != null ? ownerId : "unknown-user",
                    ticket.getId()
            );
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cloudinary upload missing secure URL");
            }
            return secureUrl.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload ticket attachment", e);
        }
    }
}
