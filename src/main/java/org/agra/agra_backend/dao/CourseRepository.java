package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByCountry(String country);
    List<Course> findByDomain(String domain);
}
