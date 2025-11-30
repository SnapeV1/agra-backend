package org.agra.agra_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketEventPayload {
    private String type;
    private String ticketId;
    private Ticket ticket;
    private TicketMessage message;
}

