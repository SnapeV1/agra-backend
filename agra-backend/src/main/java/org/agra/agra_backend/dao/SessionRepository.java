package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends MongoRepository<Session, String> {
    List<Session> findByCourseIdAndStartTimeAfterOrderByStartTimeAsc(String courseId, Instant after);
    Optional<Session> findByIdAndCourseId(String id, String courseId);
}
