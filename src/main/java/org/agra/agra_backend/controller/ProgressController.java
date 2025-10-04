package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CourseProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/progress")
@CrossOrigin(origins = "*")
public class ProgressController {

    private final CourseProgressService courseProgressService;

    public ProgressController(CourseProgressService courseProgressService) {
        this.courseProgressService = courseProgressService;
    }

    @PutMapping("/lesson/progress")
    public ResponseEntity<?> updateLessonProgress(
            @RequestBody Map<String, Object> progressData,
            Authentication authentication) {
        try {
            // Log the incoming request data for debugging
            System.out.println("Received progress data: " + progressData);
            
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Extract data from request body - handle both old and new formats
            String courseId = (String) progressData.get("courseId");
            String lessonId = (String) progressData.get("lessonId");
            Object timeSpentObj = progressData.get("timeSpent");
            String lastAccessedAt = (String) progressData.get("lastAccessedAt");
            
            System.out.println("Extracted courseId: " + courseId + ", lessonId: " + lessonId + 
                             ", timeSpent: " + timeSpentObj + ", lastAccessedAt: " + lastAccessedAt);
            
            if (courseId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "courseId is required", "received", progressData));
            }

            // Handle timeSpent validation
            if (timeSpentObj != null) {
                try {
                    double timeSpent;
                    if (timeSpentObj instanceof Integer) {
                        timeSpent = ((Integer) timeSpentObj).doubleValue();
                    } else if (timeSpentObj instanceof Double) {
                        timeSpent = (Double) timeSpentObj;
                    } else if (timeSpentObj instanceof String) {
                        timeSpent = Double.parseDouble((String) timeSpentObj);
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "timeSpent must be a number"));
                    }

                    if (timeSpent < 0) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "timeSpent cannot be negative"));
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "timeSpent must be a valid number"));
                }
            }

            // Update current lesson if lessonId is provided
            CourseProgress updatedProgress;
            if (lessonId != null && !lessonId.trim().isEmpty()) {
                updatedProgress = courseProgressService.setCurrentLesson(userId, courseId, lessonId);
            } else {
                // If no lessonId, just ensure user is enrolled
                Optional<CourseProgress> progressOpt = courseProgressService.getEnrollmentStatus(userId, courseId);
                if (progressOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "User is not enrolled in this course"));
                }
                updatedProgress = progressOpt.get();
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Lesson progress updated successfully",
                    "courseId", updatedProgress.getCourseId(),
                    "currentLessonId", updatedProgress.getCurrentLessonId(),
                    "progressPercentage", updatedProgress.getProgressPercentage(),
                    "completed", updatedProgress.isCompleted(),
                    "startedAt", updatedProgress.getStartedAt()
            ));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not enrolled")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }
            System.err.println("Runtime error in updateLessonProgress: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update lesson progress: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error in updateLessonProgress: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update lesson progress"));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseProgress(
            @PathVariable String courseId,
            Authentication authentication) {
        System.out.println("GET /api/progress/course/" + courseId + " - Request received");
        
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                System.err.println("GET /api/progress/course/" + courseId + " - Authentication failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();
            
            System.out.println("GET /api/progress/course/" + courseId + " - User: " + userId);

            Optional<CourseProgress> progressOpt = courseProgressService.getEnrollmentStatus(userId, courseId);
            
            if (progressOpt.isPresent()) {
                CourseProgress progress = progressOpt.get();
                
                System.out.println("GET /api/progress/course/" + courseId + " - Found progress:");
                System.out.println("  - Progress percentage: " + progress.getProgressPercentage());
                System.out.println("  - Completed: " + progress.isCompleted());
                System.out.println("  - Started at: " + progress.getStartedAt());
                System.out.println("  - Current lesson: " + progress.getCurrentLessonId());
                System.out.println("  - Completed lessons: " + progress.getCompletedLessons());
                System.out.println("  - Lesson completion dates: " + progress.getLessonCompletionDates());
                
                Map<String, Object> response = new HashMap<>();
                response.put("courseId", progress.getCourseId());
                response.put("progressPercentage", progress.getProgressPercentage());
                response.put("completed", progress.isCompleted());
                response.put("enrolledAt", progress.getEnrolledAt());
                response.put("startedAt", progress.getStartedAt()); // Can be null
                response.put("currentLessonId", progress.getCurrentLessonId()); // Can be null
                response.put("completedLessons", progress.getCompletedLessons() != null ? progress.getCompletedLessons() : new ArrayList<>());
                response.put("lessonCompletionDates", progress.getLessonCompletionDates()); // Add completion dates
                response.put("certificateUrl", progress.getCertificateUrl()); // Can be null
                
                System.out.println("GET /api/progress/course/" + courseId + " - Returning response with " + 
                                 (progress.getCompletedLessons() != null ? progress.getCompletedLessons().size() : 0) + 
                                 " completed lessons");
                
                return ResponseEntity.ok(response);
            } else {
                System.out.println("GET /api/progress/course/" + courseId + " - User not enrolled");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }

        } catch (Exception e) {
            System.err.println("GET /api/progress/course/" + courseId + " - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve progress: " + e.getMessage()));
        }
    }

    @PostMapping("/lesson/complete")
    public ResponseEntity<?> markLessonComplete(
            @RequestBody Map<String, Object> lessonData,
            Authentication authentication) {
        try {
            // Log the incoming request data for debugging
            System.out.println("Received lesson completion data: " + lessonData);
            
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Extract data from request body
            String courseId = (String) lessonData.get("courseId");
            String lessonId = (String) lessonData.get("lessonId");
            String completedAtStr = (String) lessonData.get("completedAt");
            
            System.out.println("Extracted courseId: " + courseId + ", lessonId: " + lessonId + 
                             ", completedAt: " + completedAtStr + ", userId: " + userId);
            
            if (courseId == null || lessonId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "courseId and lessonId are required", "received", lessonData));
            }

            // Validate lessonId is not empty
            if (lessonId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "lessonId cannot be empty"));
            }

            Date completedAt = completedAtStr != null ? new Date() : new Date();

            // Check if user is enrolled before attempting to mark lesson complete
            Optional<CourseProgress> existingProgress = courseProgressService.getEnrollmentStatus(userId, courseId);
            if (existingProgress.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }

            System.out.println("User is enrolled. Current completed lessons: " + 
                             existingProgress.get().getCompletedLessons());

            CourseProgress updatedProgress = courseProgressService.markLessonComplete(userId, courseId, lessonId, completedAt);

            System.out.println("Lesson marked complete. Updated completed lessons: " + 
                             updatedProgress.getCompletedLessons());

            // Verify the lesson was actually added to completed list
            boolean lessonWasAdded = updatedProgress.getCompletedLessons().contains(lessonId);
            System.out.println("Lesson " + lessonId + " was added to completed list: " + lessonWasAdded);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Lesson marked as complete");
            response.put("courseId", updatedProgress.getCourseId());
            response.put("lessonId", lessonId);
            response.put("completedLessons", updatedProgress.getCompletedLessons());
            response.put("completedAt", completedAt);
            response.put("progressPercentage", updatedProgress.getProgressPercentage());
            response.put("lessonCompletionDates", updatedProgress.getLessonCompletionDates());
            response.put("success", lessonWasAdded);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Runtime error in markLessonComplete: " + e.getMessage());
            e.printStackTrace();
            if (e.getMessage().contains("not enrolled")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark lesson as complete: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error in markLessonComplete: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark lesson as complete"));
        }
    }

    @PutMapping("/current-lesson")
    public ResponseEntity<?> setCurrentLesson(
            @RequestBody Map<String, Object> lessonData,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Extract data from request body
            String courseId = (String) lessonData.get("courseId");
            String lessonId = (String) lessonData.get("lessonId");
            
            if (courseId == null || lessonId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "courseId and lessonId are required"));
            }

            CourseProgress updatedProgress = courseProgressService.setCurrentLesson(userId, courseId, lessonId);

            return ResponseEntity.ok(Map.of(
                    "message", "Current lesson updated successfully",
                    "courseId", updatedProgress.getCourseId(),
                    "currentLessonId", updatedProgress.getCurrentLessonId(),
                    "startedAt", updatedProgress.getStartedAt()
            ));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not enrolled")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set current lesson"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set current lesson"));
        }
    }

    @PostMapping("/course/complete")
    public ResponseEntity<?> markCourseComplete(
            @RequestBody Map<String, Object> courseData,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            // Extract data from request body
            String courseId = (String) courseData.get("courseId");
            String completedAtStr = (String) courseData.get("completedAt");
            
            if (courseId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "courseId is required"));
            }

            Date completedAt = completedAtStr != null ? new Date() : new Date();

            CourseProgress updatedProgress = courseProgressService.markCourseComplete(userId, courseId, completedAt);

            return ResponseEntity.ok(Map.of(
                    "message", "Course marked as complete",
                    "courseId", updatedProgress.getCourseId(),
                    "completed", updatedProgress.isCompleted(),
                    "progressPercentage", updatedProgress.getProgressPercentage(),
                    "completedAt", completedAt,
                    "certificateUrl", updatedProgress.getCertificateUrl()
            ));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not enrolled")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "User is not enrolled in this course"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark course as complete"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark course as complete"));
        }
    }

    @GetMapping("/certificate/{courseId}")
    public ResponseEntity<?> getCertificate(
            @PathVariable String courseId,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            return courseProgressService.getEnrollmentStatus(userId, courseId)
                    .map(progress -> {
                        if (progress.isCompleted() && progress.getCertificateUrl() != null) {
                            return ResponseEntity.ok(Map.of(
                                    "certificateUrl", progress.getCertificateUrl(),
                                    "courseId", progress.getCourseId(),
                                    "userId", userId,
                                    "completedAt", progress.getStartedAt(), // This should be completion date
                                    "message", "Certificate available for download"
                            ));
                        } else if (!progress.isCompleted()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("error", "Course not completed yet"));
                        } else {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Certificate not found"));
                        }
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User is not enrolled in this course")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve certificate"));
        }
    }

    @PostMapping("/certificate/generate/{courseId}")
    public ResponseEntity<?> generateCertificate(
            @PathVariable String courseId,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            return courseProgressService.getEnrollmentStatus(userId, courseId)
                    .map(progress -> {
                        if (progress.isCompleted()) {
                            // Generate or update certificate URL
                            String certificateUrl = "https://certificates.agra.com/course/" + courseId + "/user/" + userId + "/certificate.pdf";
                            progress.setCertificateUrl(certificateUrl);
                            courseProgressService.updateProgress(userId, courseId, progress.getProgressPercentage());
                            
                            return ResponseEntity.ok(Map.of(
                                    "message", "Certificate generated successfully",
                                    "certificateUrl", certificateUrl,
                                    "courseId", courseId,
                                    "userId", userId
                            ));
                        } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("error", "Course must be completed before generating certificate"));
                        }
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User is not enrolled in this course")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate certificate"));
        }
    }
}