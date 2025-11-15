package org.agra.agra_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.agra.agra_backend.model.Ticket;
import org.agra.agra_backend.model.TicketMessage;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketThreadResponse {
    private Ticket ticket;
    private List<TicketMessage> messages;
}

