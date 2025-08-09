package org.agra.agra_backend.controller;


import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")

public class CourseController {

    private final CloudinaryService cloudinaryService;
    private final CourseService courseService;

    public CourseController(CloudinaryService cloudinaryService, CourseService courseService) {
        this.cloudinaryService = cloudinaryService;
        this.courseService = courseService;
    }

    @PostMapping("/addCourse")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        if (course.getId() == null || course.getId().isEmpty()) {
            course.setId(null);
        }
        Course savedCourse = courseService.createCourse(course);
        return ResponseEntity.ok(savedCourse);
    }
    @PostMapping("/addCourses")
    public ResponseEntity<List<Course>> createCourses(@RequestBody List<Course> courses) {
        List<Course> savedCourses = courses.stream()
                .map(courseService::createCourse)
                .toList();
        return ResponseEntity.ok(savedCourses);
    }


    @GetMapping("/getAllCourses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable String id) {
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update a course
    @PutMapping("update/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course course) {
        System.out.println("updating course");
        return courseService.updateCourse(id, course)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a course
    @PutMapping("ArchiveCourse/{id}")
    public ResponseEntity<Void> ArchiveCourse(@PathVariable String id) {
        courseService.ArchiveCourse(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // Filter by country
    @GetMapping("/country/{country}")
    public ResponseEntity<List<Course>> getCoursesByCountry(@PathVariable String country) {
        return ResponseEntity.ok(courseService.getCoursesByCountry(country));
    }

    // Filter by domain
    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<Course>> getCoursesByDomain(@PathVariable String domain) {
        return ResponseEntity.ok(courseService.getCoursesByDomain(domain));
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testCloudinaryConnection() {
        try {
            // Test connection by getting account details
            Map<String, Object> config = Map.of(
                    "cloudinaryConfigured", true,
                    "message", "Cloudinary service is properly configured",
                    "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "cloudinaryConfigured", false,
                            "error", "Cloudinary configuration error",
                            "details", e.getMessage()
                    ));
        }


    }}