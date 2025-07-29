package org.agra.agra_backend.service;


import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;


    public Course createCourse(Course course) {
        course.setCreatedAt(new java.util.Date());
        course.setUpdatedAt(new java.util.Date());
        return courseRepository.save(course);
    }


    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }


    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id);
    }


    public Optional<Course> updateCourse(String id, Course updatedCourse) {
        return courseRepository.findById(id).map(existingCourse -> {
            existingCourse.setTitle(updatedCourse.getTitle());
            existingCourse.setDescription(updatedCourse.getDescription());
            existingCourse.setDomain(updatedCourse.getDomain());
            existingCourse.setCountry(updatedCourse.getCountry());
            existingCourse.setUpdatedAt(new java.util.Date());
            return courseRepository.save(existingCourse);
        });
    }

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }


    public List<Course> getCoursesByCountry(String country) {
        return courseRepository.findByCountry(country);
    }


    public List<Course> getCoursesByDomain(String domain) {
        return courseRepository.findByDomain(domain);
    }
}
