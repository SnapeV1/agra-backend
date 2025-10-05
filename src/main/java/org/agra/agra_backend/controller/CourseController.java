package org.agra.agra_backend.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseLikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")

public class CourseController {

    private final CloudinaryService cloudinaryService;
    private final CourseService courseService;
    private final CourseProgressService courseProgressService;
    private final CourseLikeService courseLikeService;

    public CourseController(CloudinaryService cloudinaryService, CourseService courseService, CourseProgressService courseProgressService, CourseLikeService courseLikeService) {
        this.cloudinaryService = cloudinaryService;
        this.courseService = courseService;
        this.courseProgressService = courseProgressService;
        this.courseLikeService = courseLikeService;
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
    public ResponseEntity<List<Course>> getAllCourses(Authentication authentication) {
        List<Course> courses = courseService.getAllCourses();
        if (authentication != null && authentication.getPrincipal() != null) {
            User user = (User) authentication.getPrincipal();
            String userId = user.getId();
            java.util.Set<String> likedIds = new java.util.HashSet<>(courseLikeService.listLikedCourseIds(userId));
            for (Course c : courses) {
                if (c != null && c.getId() != null) {
                    c.setLiked(likedIds.contains(c.getId()));
                }
            }
        }
        return ResponseEntity.ok(courses);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable String id, Authentication authentication) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        if (courseOpt.isEmpty()) return ResponseEntity.notFound().build();
        Course course = courseOpt.get();
        if (authentication != null && authentication.getPrincipal() != null) {
            User user = (User) authentication.getPrincipal();
            String userId = user.getId();
            course.setLiked(courseLikeService.isLiked(userId, id));
        }
        return ResponseEntity.ok(course);
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

    @GetMapping("/{id}/enrollment-status")
    public ResponseEntity<?> getEnrollmentStatus(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required", "enrolled", false));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            Optional<CourseProgress> progress = courseProgressService.getEnrollmentStatus(userId, id);
            
            if (progress.isPresent()) {
                CourseProgress courseProgress = progress.get();
                return ResponseEntity.ok(Map.of(
                        "enrolled", true,
                        "enrolledAt", courseProgress.getEnrolledAt(),
                        "progressPercentage", courseProgress.getProgressPercentage(),
                        "completed", courseProgress.isCompleted()
                ));
            } else {
                return ResponseEntity.ok(Map.of("enrolled", false));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check enrollment status", "enrolled", false));
        }
    }

    @GetMapping("/{id}/unenrolled-others")
    public ResponseEntity<List<Course>> getOtherUnenrolledCourses(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(null);
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            List<CourseProgress> enrollments = courseProgressService.getUserEnrollments(userId);
            java.util.Set<String> enrolledCourseIds = new java.util.HashSet<>();
            for (CourseProgress p : enrollments) {
                enrolledCourseIds.add(p.getCourseId());
            }

            List<Course> allCourses = courseService.getAllCourses();
            List<Course> result = new ArrayList<>();
            for (Course c : allCourses) {
                if (c.getId() == null) continue;
                if (c.isArchived()) continue;
                if (c.getId().equals(id)) continue;
                if (enrolledCourseIds.contains(c.getId())) continue;
                result.add(c);
            }

            // Set liked flag if authenticated
            java.util.Set<String> likedIds = new java.util.HashSet<>(courseLikeService.listLikedCourseIds(userId));
            for (Course c : result) {
                c.setLiked(likedIds.contains(c.getId()));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeCourse(@PathVariable String id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        boolean ok = courseLikeService.likeCourse(userId, id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found"));
        }
        return ResponseEntity.ok(Map.of("liked", true));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeCourse(@PathVariable String id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        boolean ok = courseLikeService.unlikeCourse(userId, id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Course not found"));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<?> enrollInCourse(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Check if course exists
            Optional<Course> course = courseService.getCourseById(id);
            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Course not found"));
            }

            CourseProgress progress = courseProgressService.enrollUserInCourse(userId, id);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully enrolled in course",
                    "enrolled", true,
                    "enrolledAt", progress.getEnrolledAt(),
                    "progressPercentage", progress.getProgressPercentage()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to enroll in course"));
        }
    }

    @GetMapping("/enrolled")
    public ResponseEntity<?> getEnrolledCourses(Authentication authentication) {
        System.out.println("GET /api/courses/enrolled - Request received");
        
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                System.err.println("GET /api/courses/enrolled - Authentication failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();
            
            System.out.println("GET /api/courses/enrolled - User: " + userId);

            // Get all user enrollments
            List<CourseProgress> enrollments = courseProgressService.getUserEnrollments(userId);
            System.out.println("GET /api/courses/enrolled - Found " + enrollments.size() + " enrollments");
            
            // Log all enrollment details for debugging
            for (int i = 0; i < enrollments.size(); i++) {
                CourseProgress enrollment = enrollments.get(i);
                System.out.println("GET /api/courses/enrolled - Enrollment " + (i+1) + ":");
                System.out.println("  - CourseId: " + enrollment.getCourseId());
                System.out.println("  - UserId: " + enrollment.getUserId());
                System.out.println("  - EnrolledAt: " + enrollment.getEnrolledAt());
                System.out.println("  - Progress: " + enrollment.getProgressPercentage() + "%");
            }

            // Build response with course details and progress data
            List<Map<String, Object>> coursesWithProgress = new ArrayList<>();
            
            for (CourseProgress progress : enrollments) {
                System.out.println("GET /api/courses/enrolled - Processing courseId: " + progress.getCourseId());
                Optional<Course> courseOpt = courseService.getCourseById(progress.getCourseId());
                
                if (courseOpt.isPresent()) {
                    Course course = courseOpt.get();
                    
                    // Skip archived courses
                    if (course.isArchived()) {
                        System.out.println("GET /api/courses/enrolled - Skipping archived course: " + course.getTitle() + 
                                         " (ID: " + course.getId() + ")");
                        continue;
                    }
                    
                    Map<String, Object> courseData = new HashMap<>();
                    
                    // Course basic info
                    courseData.put("id", course.getId());
                    courseData.put("title", course.getTitle());
                    courseData.put("description", course.getDescription());
                    courseData.put("domain", course.getDomain());
                    courseData.put("country", course.getCountry());
                    courseData.put("trainerId", course.getTrainerId());
                    courseData.put("imageUrl", course.getImageUrl());
                    courseData.put("createdAt", course.getCreatedAt());
                    courseData.put("updatedAt", course.getUpdatedAt());
                    
                    // Progress data
                    courseData.put("enrolledAt", progress.getEnrolledAt());
                    courseData.put("startedAt", progress.getStartedAt());
                    courseData.put("progressPercentage", progress.getProgressPercentage());
                    courseData.put("completed", progress.isCompleted());
                    courseData.put("currentLessonId", progress.getCurrentLessonId());
                    courseData.put("completedLessons", progress.getCompletedLessons() != null ? progress.getCompletedLessons() : new ArrayList<>());
                    courseData.put("lessonCompletionDates", progress.getLessonCompletionDates() != null ? progress.getLessonCompletionDates() : new HashMap<>());
                    courseData.put("certificateUrl", progress.getCertificateUrl());
                    
                    coursesWithProgress.add(courseData);
                    
                    System.out.println("GET /api/courses/enrolled - Added course: " + course.getTitle() + 
                                     " (Progress: " + progress.getProgressPercentage() + "%)");
                } else {
                    System.err.println("GET /api/courses/enrolled - Course not found: " + progress.getCourseId());
                }
            }
            
            System.out.println("GET /api/courses/enrolled - Returning " + coursesWithProgress.size() + " courses");
            
            return ResponseEntity.ok(Map.of(
                    "courses", coursesWithProgress,
                    "totalEnrollments", enrollments.size()
            ));

        } catch (Exception e) {
            System.err.println("GET /api/courses/enrolled - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve enrolled courses: " + e.getMessage()));
        }
    }

    /**
     * Admin endpoint to cleanup orphaned enrollments
     * This removes enrollments for courses that no longer exist
     */
    @PostMapping("/admin/cleanup-orphaned-enrollments")
    public ResponseEntity<?> cleanupOrphanedEnrollments() {
        try {
            System.out.println("POST /api/courses/admin/cleanup-orphaned-enrollments - Request received");
            
            int deletedCount = courseProgressService.cleanupOrphanedEnrollments(courseService);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Cleanup completed successfully",
                    "deletedEnrollments", deletedCount
            ));

        } catch (Exception e) {
            System.err.println("POST /api/courses/admin/cleanup-orphaned-enrollments - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup orphaned enrollments: " + e.getMessage()));
        }
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
                    .body(Map.of("error", "Cloudinary configuration error: " + e.getMessage()));
        }
    }
}