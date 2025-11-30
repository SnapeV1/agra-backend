package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByUserInfo_IdOrderByCreatedAtDesc(String userId);
    List<Ticket> findByAdminInfo_IdOrderByCreatedAtDesc(String adminId);
    List<Ticket> findAllByOrderByCreatedAtDesc();

    Optional<Ticket> findByIdAndUserInfo_Id(String id, String userId);
    Optional<Ticket> findByIdAndAdminInfo_Id(String id, String adminId);
}
