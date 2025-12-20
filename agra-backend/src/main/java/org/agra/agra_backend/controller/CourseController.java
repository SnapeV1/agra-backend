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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ENROLLED = "enrolled";
    private static final String KEY_ENROLLED_AT = "enrolledAt";
    private static final String KEY_PROGRESS_PERCENTAGE = "progressPercentage";
    private static final String MSG_AUTH_REQUIRED = "Authentication required";
    private static final String MSG_COURSE_NOT_FOUND = "Course not found";

    private final SimpMessagingTemplate messagingTemplate;

    private final CloudinaryService cloudinaryService;
    private final CourseService courseService;
    private final CourseProgressService courseProgressService;
    private final CourseLikeService courseLikeService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public CourseController(SimpMessagingTemplate messagingTemplate, CloudinaryService cloudinaryService, CourseService courseService,
                            CourseProgressService courseProgressService, CourseLikeService courseLikeService,
                            NotificationRepository notificationRepository, NotificationService notificationService) {
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
            @RequestPart("course") String courseJson,
            @RequestPart(value = "image", required = false) MultipartFile courseImage) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Course course = objectMapper.readValue(courseJson, Course.class);
            Course localizedForNotification = courseService.localizeCourse(course, LocaleContextHolder.getLocale());
            Notification notification = new Notification(
                    UUID.randomUUID().toString(),
                    "New post published: " + localizedForNotification.getTitle(),
                    NotificationType.COURSE,
                    LocalDateTime.now()
            );
            Course createdCourse = courseService.createCourse(course, courseImage);
            notificationRepository.save(notification);
            // create unseen status for all users
            notificationService.createStatusesForAllUsers(notification);

            messagingTemplate.convertAndSend("/topic/notifications", notification);


            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(courseService.localizeCourse(createdCourse, LocaleContextHolder.getLocale()));
        } catch (JsonProcessingException e) {
            log.warn("Error parsing course JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage());
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

        return ResponseEntity.ok(courseService.localizeCourses(courses, LocaleContextHolder.getLocale()));
    }

    @GetMapping("/getActiveCourses")
    public ResponseEntity<List<Course>> getActiveCourses(Authentication authentication) {
        List<Course> courses = courseService.getActiveCourses();
        log.debug("Active courses: {}", courses);

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

        return ResponseEntity.ok(courseService.localizeCourses(courses, LocaleContextHolder.getLocale()));
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
        return ResponseEntity.ok(courseService.localizeCourse(course, LocaleContextHolder.getLocale()));
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
                    .map(updated -> courseService.localizeCourse(updated, LocaleContextHolder.getLocale()))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (JsonProcessingException e) {
            log.warn("Error parsing course JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("ArchiveCourse/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> archiveCourse(@PathVariable String id) {
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
        List<Course> courses = courseService.getCoursesByCountry(country);
        return ResponseEntity.ok(courseService.localizeCourses(courses, LocaleContextHolder.getLocale()));
    }

    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<Course>> getCoursesByDomain(@PathVariable String domain) {
        List<Course> courses = courseService.getCoursesByDomain(domain);
        return ResponseEntity.ok(courseService.localizeCourses(courses, LocaleContextHolder.getLocale()));
    }

    @GetMapping("/{id}/enrollment-status")
    public ResponseEntity<Object> getEnrollmentStatus(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED, KEY_ENROLLED, false));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            Optional<CourseProgress> progress = courseProgressService.getEnrollmentStatus(userId, id);
            
            if (progress.isPresent()) {
                CourseProgress courseProgress = progress.get();
                return ResponseEntity.ok(Map.of(
                        KEY_ENROLLED, true,
                        KEY_ENROLLED_AT, courseProgress.getEnrolledAt(),
                        KEY_PROGRESS_PERCENTAGE, courseProgress.getProgressPercentage(),
                        "completed", courseProgress.isCompleted()
                ));
            } else {
                return ResponseEntity.ok(Map.of(KEY_ENROLLED, false));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to check enrollment status", KEY_ENROLLED, false));
        }
    }

    @GetMapping("/{id}/unenrolled-others")
    public ResponseEntity<Object> getOtherUnenrolledCourses(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            List<CourseProgress> enrollments = courseProgressService.getUserEnrollments(userId);
            java.util.Set<String> enrolledCourseIds = enrollments.stream()
                    .map(CourseProgress::getCourseId)
                    .collect(java.util.stream.Collectors.toSet());

            List<Course> allCourses = courseService.getAllCourses();
            List<Course> result = allCourses.stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.getId() != null)
                    .filter(c -> !c.isArchived())
                    .filter(c -> !c.getId().equals(id))
                    .filter(c -> !enrolledCourseIds.contains(c.getId()))
                    .toList();

            // Set liked flag if authenticated
            java.util.Set<String> likedIds = new java.util.HashSet<>(courseLikeService.listLikedCourseIds(userId));
            for (Course c : result) {
                c.setLiked(likedIds.contains(c.getId()));
            }

            return ResponseEntity.ok(courseService.localizeCourses(result, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Object> likeCourse(@PathVariable String id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        boolean ok = courseLikeService.likeCourse(userId, id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(KEY_ERROR, MSG_COURSE_NOT_FOUND));
        }
        return ResponseEntity.ok(Map.of("liked", true));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Object> unlikeCourse(@PathVariable String id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        boolean ok = courseLikeService.unlikeCourse(userId, id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(KEY_ERROR, MSG_COURSE_NOT_FOUND));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Object> enrollInCourse(@PathVariable String id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Check if course exists
            Optional<Course> course = courseService.getCourseById(id);
            if (course.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, MSG_COURSE_NOT_FOUND));
            }

            CourseProgress progress = courseProgressService.enrollUserInCourse(userId, id);
            
            return ResponseEntity.ok(Map.of(
                    KEY_MESSAGE, "Successfully enrolled in course",
                    KEY_ENROLLED, true,
                    KEY_ENROLLED_AT, progress.getEnrolledAt(),
                    KEY_PROGRESS_PERCENTAGE, progress.getProgressPercentage()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to enroll in course"));
        }
    }

    @GetMapping("/enrolled")
    public ResponseEntity<Object> getEnrolledCourses(Authentication authentication) {
        log.debug("GET /api/courses/enrolled - Request received");
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("GET /api/courses/enrolled - Authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
        }

        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        log.debug("GET /api/courses/enrolled - User: {}", userId);

        try {
            List<CourseProgress> enrollments = courseProgressService.getUserEnrollments(userId);
            log.debug("GET /api/courses/enrolled - Found {} enrollments", enrollments.size());
            List<Map<String, Object>> coursesWithProgress = buildCoursesWithProgress(enrollments);

            log.debug("GET /api/courses/enrolled - Returning {} courses", coursesWithProgress.size());
            return ResponseEntity.ok(Map.of(
                    "courses", coursesWithProgress,
                    "totalEnrollments", enrollments.size()
            ));
        } catch (Exception e) {
            log.error("GET /api/courses/enrolled - Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to retrieve enrolled courses: " + e.getMessage()));
        }
    }

    private List<Map<String, Object>> buildCoursesWithProgress(List<CourseProgress> enrollments) {
        List<Map<String, Object>> coursesWithProgress = new ArrayList<>();
        for (int i = 0; i < enrollments.size(); i++) {
            CourseProgress enrollment = enrollments.get(i);
            log.debug("GET /api/courses/enrolled - Enrollment {}: courseId={}, userId={}, enrolledAt={}, progress={}%",
                    i + 1, enrollment.getCourseId(), enrollment.getUserId(),
                    enrollment.getEnrolledAt(), enrollment.getProgressPercentage());
            buildCourseData(enrollment).ifPresent(coursesWithProgress::add);
        }
        return coursesWithProgress;
    }

    private Optional<Map<String, Object>> buildCourseData(CourseProgress progress) {
        log.debug("GET /api/courses/enrolled - Processing courseId: {}", progress.getCourseId());
        Optional<Course> courseOpt = courseService.getCourseById(progress.getCourseId());
        if (courseOpt.isEmpty()) {
            log.warn("GET /api/courses/enrolled - Course not found: {}", progress.getCourseId());
            return Optional.empty();
        }
        Course localizedCourse = courseService.localizeCourse(courseOpt.get(), LocaleContextHolder.getLocale());
        if (localizedCourse.isArchived()) {
            log.debug("GET /api/courses/enrolled - Skipping archived course: {} (ID: {})",
                    localizedCourse.getTitle(), localizedCourse.getId());
            return Optional.empty();
        }
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("id", localizedCourse.getId());
        courseData.put("title", localizedCourse.getTitle());
        courseData.put("description", localizedCourse.getDescription());
        courseData.put("domain", localizedCourse.getDomain());
        courseData.put("country", localizedCourse.getCountry());
        courseData.put("trainerId", localizedCourse.getTrainerId());
        courseData.put("imageUrl", localizedCourse.getImageUrl());
        courseData.put("createdAt", localizedCourse.getCreatedAt());
        courseData.put("updatedAt", localizedCourse.getUpdatedAt());

        courseData.put(KEY_ENROLLED_AT, progress.getEnrolledAt());
        courseData.put("startedAt", progress.getStartedAt());
        courseData.put(KEY_PROGRESS_PERCENTAGE, progress.getProgressPercentage());
        courseData.put("completed", progress.isCompleted());
        courseData.put("currentLessonId", progress.getCurrentLessonId());
        courseData.put("completedLessons", progress.getCompletedLessons() != null ? progress.getCompletedLessons() : new ArrayList<>());
        courseData.put("lessonCompletionDates", progress.getLessonCompletionDates() != null ? progress.getLessonCompletionDates() : new HashMap<>());
        courseData.put("certificateUrl", progress.getCertificateUrl());

        log.debug("GET /api/courses/enrolled - Added course: {} (Progress: {}%)",
                localizedCourse.getTitle(), progress.getProgressPercentage());
        return Optional.of(courseData);
    }

    /**
     * Admin endpoint to cleanup orphaned enrollments
     * This removes enrollments for courses that no longer exist
     */
    @PostMapping("/admin/cleanup-orphaned-enrollments")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> cleanupOrphanedEnrollments() {
        try {
            log.info("POST /api/courses/admin/cleanup-orphaned-enrollments - Request received");
            
            int deletedCount = courseProgressService.cleanupOrphanedEnrollments(courseService);
            
            return ResponseEntity.ok(Map.of(
                    KEY_MESSAGE, "Cleanup completed successfully",
                    "deletedEnrollments", deletedCount
            ));

        } catch (Exception e) {
            log.error("POST /api/courses/admin/cleanup-orphaned-enrollments - Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to cleanup orphaned enrollments: " + e.getMessage()));
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<Object> testCloudinaryConnection() {
        try {
            Map<String, Object> config = Map.of(
                    "cloudinaryConfigured", true,
                    KEY_MESSAGE, "Cloudinary service is properly configured",
                    "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Cloudinary configuration error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> uploadCourseFile(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "File is required"));
            }

            Optional<Course> courseOpt = courseService.getCourseById(id);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, MSG_COURSE_NOT_FOUND));
            }

            Course course = courseOpt.get();

            String folderPath = "courses/" + id + "/files";
            log.info("[CourseController] Uploading file as image - courseId={}, name={}, contentType={}, size={} bytes, targetFolder={}",
                    id, file.getOriginalFilename(), file.getContentType(), file.getSize(), folderPath);
            Map<String, Object> uploadResult = cloudinaryService.uploadImageToFolder(file, folderPath);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String originalName = file.getOriginalFilename();
            long size = file.getSize();
            String formatFromCloudinary = uploadResult.get("format") != null ? uploadResult.get("format").toString() : null;
            String inferredType = inferFileExtension(originalName, formatFromCloudinary, url, publicId);

            log.info("[CourseController] Cloudinary result (image) - resource_type={}, format={}, secure_url={}, public_id={}, bytes={}",
                    uploadResult.get("resource_type"), uploadResult.get("format"), uploadResult.get("secure_url"),
                    uploadResult.get("public_id"), uploadResult.get("bytes"));

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

            log.info("[CourseController] Saved CourseFile - id={}, name={}, type={}, size={}, url={}, publicId={}",
                    courseFile.getId(), courseFile.getName(), courseFile.getType(), courseFile.getSize(),
                    courseFile.getUrl(), courseFile.getPublicId());

            return ResponseEntity.ok(courseFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to upload file: " + e.getMessage()));
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
    public ResponseEntity<Object> deleteCourseFile(@PathVariable String id, @PathVariable String fileId) {
        try {
            Optional<Course> courseOpt = courseService.getCourseById(id);
            if (courseOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, MSG_COURSE_NOT_FOUND));
            }

            Course course = courseOpt.get();
            if (course.getFiles() == null || course.getFiles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, "File not found"));
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
                        .body(Map.of(KEY_ERROR, "File not found"));
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
                    .body(Map.of(KEY_ERROR, "Failed to delete file: " + e.getMessage()));
        }
    }

    
}
