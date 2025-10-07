package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.MeetingRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRoomRepository extends MongoRepository<MeetingRoom, String> {
}