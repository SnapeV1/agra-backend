package org.agra.agra_backend.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping(value="/addCourse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Course> createCourse(
            @RequestPart("course") Course course,
            @RequestPart(value = "image", required = false) MultipartFile courseImage) {
        try {

            Course createdCourse = courseService.createCourse(course, courseImage);
            return ResponseEntity.ok(createdCourse);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
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

    @PutMapping(value = "updateCourse/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Course> updateCourse(
            @PathVariable String id,
            @RequestPart("course") String courseJson,
            @RequestPart(value = "image", required = false) MultipartFile courseImage) {


        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Course course = objectMapper.readValue(courseJson, Course.class);

            return courseService.updateCourse(id, course, courseImage)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (JsonProcessingException e) {
            System.err.println("Error parsing course JSON: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            System.err.println("Error uploading image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


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

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Course>> getCoursesByCountry(@PathVariable String country) {
        return ResponseEntity.ok(courseService.getCoursesByCountry(country));
    }

    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<Course>> getCoursesByDomain(@PathVariable String domain) {
        return ResponseEntity.ok(courseService.getCoursesByDomain(domain));
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testCloudinaryConnection() {
        try {
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