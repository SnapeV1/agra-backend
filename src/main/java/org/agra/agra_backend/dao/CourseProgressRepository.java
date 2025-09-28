package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.CourseProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CourseProgressRepository extends MongoRepository<CourseProgress, String> {
    Optional<CourseProgress> findByUserIdAndCourseId(String userId, String courseId);
    List<CourseProgress> findByUserId(String userId);
    List<CourseProgress> findByCourseId(String courseId);
    boolean existsByUserIdAndCourseId(String userId, String courseId);
}