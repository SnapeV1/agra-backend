package org.agra.agra_backend.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.model.*;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseLikeService;
import org.agra.agra_backend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")

public class CourseController {
    private SimpMessagingTemplate messagingTemplate;

    private final CloudinaryService cloudinaryService;
    private final CourseService courseService;
    private final CourseProgressService courseProgressService;
    private final CourseLikeService courseLikeService;
    private NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public CourseController(SimpMessagingTemplate messagingTemplate,CloudinaryService cloudinaryService, CourseService courseService, CourseProgressService courseProgressService, CourseLikeService courseLikeService
    , NotificationRepository notificationRepository, NotificationService notificationService) {
        this.cloudinaryService = cloudinaryService;
        this.courseService = courseService;
        this.courseProgressService = courseProgressService;
        this.courseLikeService = courseLikeService;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @PostMapping(value="/addCourse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> createCourse(
            @RequestPart("course") Course course,
            @RequestPart(value = "image", required = false) MultipartFile courseImage) {
        try {
            Notification notification = new Notification(
                    UUID.randomUUID().toString(),
                    "New post published: " + course.getTitle(),
                    NotificationType.COURSE,
                    LocalDateTime.now()
            );
            Course createdCourse = courseService.createCourse(course, courseImage);
            notificationRepository.save(notification);
            // create unseen status for all users
            notificationService.createStatusesForAllUsers(notification);

            messagingTemplate.convertAndSend("/topic/notifications", notification);


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

    @GetMapping("/getActiveCourses")
    public ResponseEntity<List<Course>> getActiveCourses(Authentication authentication) {
        List<Course> courses = courseService.getActiveCourses();
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ArchiveCourse(@PathVariable String id) {
        courseService.ArchiveCourse(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("delete/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCourseFile(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
            }

            Optional<Course> courseOpt = courseService.getCourseById(id);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Course not found"));
            }

            Course course = courseOpt.get();

            String folderPath = "courses/" + id + "/files";
            System.out.println("[CourseController] Uploading file as image - courseId=" + id
                    + ", name=" + file.getOriginalFilename()
                    + ", contentType=" + file.getContentType()
                    + ", size=" + file.getSize() + " bytes"
                    + ", targetFolder=" + folderPath + ")");
            Map<String, Object> uploadResult = cloudinaryService.uploadImageToFolder(file, folderPath);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String originalName = file.getOriginalFilename();
            long size = file.getSize();
            String formatFromCloudinary = uploadResult.get("format") != null ? uploadResult.get("format").toString() : null;
            String inferredType = inferFileExtension(originalName, formatFromCloudinary, url, publicId);

            System.out.println("[CourseController] Cloudinary result (image) - resource_type=" + uploadResult.get("resource_type")
                    + ", format=" + uploadResult.get("format")
                    + ", secure_url=" + uploadResult.get("secure_url")
                    + ", public_id=" + uploadResult.get("public_id") + 
                    ", bytes=" + uploadResult.get("bytes") + ")");

            CourseFile courseFile = new CourseFile(
                    java.util.UUID.randomUUID().toString(),
                    originalName,
                    inferredType != null ? inferredType : "file",
                    url,
                    publicId,
                    size,
                    new java.util.Date()
            );

            if (course.getFiles() == null) {
                course.setFiles(new ArrayList<>());
            }
            course.getFiles().add(courseFile);
            course.setUpdatedAt(new java.util.Date());
            courseService.save(course);

            System.out.println("[CourseController] Saved CourseFile - id=" + courseFile.getId()
                    + ", name=" + courseFile.getName()
                    + ", type=" + courseFile.getType()
                    + ", size=" + courseFile.getSize()
                    + ", url=" + courseFile.getUrl()
                    + ", publicId=" + courseFile.getPublicId() + ")");

            return ResponseEntity.ok(courseFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    private String inferFileExtension(String originalName, String formatFromCloudinary, String secureUrl, String publicId) {
        // Prefer explicit Cloudinary format if provided
        if (formatFromCloudinary != null && !formatFromCloudinary.isBlank()) {
            return formatFromCloudinary.toLowerCase();
        }
        // From original filename
        String ext = extractExt(originalName);
        if (ext != null) return ext;
        // From secure URL
        ext = extractExt(secureUrl);
        if (ext != null) return ext;
        // From publicId
        ext = extractExt(publicId);
        return ext;
    }

    private String extractExt(String nameOrUrl) {
        if (nameOrUrl == null || nameOrUrl.isBlank()) return null;
        String s = nameOrUrl;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) s = s.substring(slash + 1);
        int dot = s.lastIndexOf('.');
        if (dot > 0 && dot < s.length() - 1) {
            String ext = s.substring(dot + 1).toLowerCase();
            // Basic sanity: avoid extremely long or suspicious extensions
            if (ext.length() <= 10) return ext;
        }
        return null;
    }

    

    @DeleteMapping("/{id}/files/{fileId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourseFile(@PathVariable String id, @PathVariable String fileId) {
        try {
            Optional<Course> courseOpt = courseService.getCourseById(id);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Course not found"));
            }

            Course course = courseOpt.get();
            if (course.getFiles() == null || course.getFiles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File not found"));
            }

            CourseFile target = null;
            for (CourseFile f : course.getFiles()) {
                if (f != null && fileId.equals(f.getId())) {
                    target = f;
                    break;
                }
            }

            if (target == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File not found"));
            }

            if (target.getPublicId() != null && !target.getPublicId().isEmpty()) {
                cloudinaryService.deleteRaw(target.getPublicId());
            }

            course.getFiles().removeIf(f -> f != null && fileId.equals(f.getId()));
            course.setUpdatedAt(new java.util.Date());
            courseService.save(course);

            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    
}
