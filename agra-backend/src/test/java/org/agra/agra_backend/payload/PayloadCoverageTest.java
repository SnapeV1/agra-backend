package org.agra.agra_backend.payload;

import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadCoverageTest {

    @Test
    void loginRequestStoresFields() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("pass");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void loginResponseAliasesAndProfileCompletion() {
        User user = new User();
        user.setPassword("pass");
        user.setName("Name");
        user.setEmail("user@example.com");
        user.setPicture("pic");
        user.setPhone("123");
        user.setCountry("GH");
        user.setBirthdate(new Date());
        LoginResponse response = new LoginResponse("jwt", user, true, "reset", "refresh");

        assertThat(response.isProfileCompleted()).isTrue();
        assertThat(response.getResetTokenAlias()).isEqualTo("reset");
        assertThat(response.getPasswordTokenAlias()).isEqualTo("reset");
        assertThat(response.getVerificationTokenAlias()).isEqualTo("reset");
    }

    @Test
    void joinResponseStoresFields() {
        JoinResponse response = new JoinResponse("room", "domain", "jwt", "display", "avatar");

        assertThat(response.getRoomName()).isEqualTo("room");
    }

    @Test
    void createSessionDtoDefaults() {
        CreateSessionDto dto = new CreateSessionDto();
        dto.setTitle("Session");
        dto.setStartTime(Instant.now());
        dto.setEndTime(Instant.now().plusSeconds(3600));

        assertThat(dto.getLobbyEnabled()).isTrue();
        assertThat(dto.getRecordingEnabled()).isFalse();
    }

    @Test
    void userInfoStoresFields() {
        UserInfo info = new UserInfo("id", "name", "email", "pic", new Date());

        assertThat(info.getEmail()).isEqualTo("email");
    }

    @Test
    void attendanceEventDtoStoresFields() {
        AttendanceEventDto dto = new AttendanceEventDto();
        dto.setType(AttendanceEventDto.EventType.JOIN);
        dto.setSecondsWatched(30L);

        assertThat(dto.getType()).isEqualTo(AttendanceEventDto.EventType.JOIN);
    }

    @Test
    void ticketThreadResponseStoresTicket() {
        Ticket ticket = new Ticket();
        TicketMessage message = new TicketMessage();
        TicketThreadResponse response = new TicketThreadResponse(ticket, List.of(message));

        assertThat(response.getMessages()).hasSize(1);
    }

    @Test
    void ticketEventPayloadStoresFields() {
        TicketEventPayload payload = new TicketEventPayload("type", "ticket-1", new Ticket(), new TicketMessage());

        assertThat(payload.getTicketId()).isEqualTo("ticket-1");
    }

    @Test
    void contactFormRequestStoresFields() {
        ContactFormRequest request = new ContactFormRequest();
        request.setFullName("Test User");
        request.setEmail("user@example.com");
        request.setSubject("Subject");
        request.setMessage("Message");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void registerRequestStoresFields() {
        RegisterRequest request = new RegisterRequest();
        request.setName("User");
        request.setEmail("user@example.com");
        request.setPassword("pass");
        request.setCountry("GH");
        request.setPhone("123");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getName()).isEqualTo("User");
    }
}
