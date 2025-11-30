package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.TicketMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketMessageRepository extends MongoRepository<TicketMessage, String> {
    List<TicketMessage> findByTicketIdOrderByTimestampAsc(String ticketId);
    long countByTicketId(String ticketId);
}

