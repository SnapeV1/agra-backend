package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.CourseProgress;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CourseProgressService {

    private final CourseProgressRepository courseProgressRepository;
    private final ActivityLogService activityLogService;

    public CourseProgressService(CourseProgressRepository courseProgressRepository,
                                 ActivityLogService activityLogService) {
        this.courseProgressRepository = courseProgressRepository;
        this.activityLogService = activityLogService;
    }

    public boolean isUserEnrolledInCourse(String userId, String courseId) {
        return courseProgressRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    public Optional<CourseProgress> getEnrollmentStatus(String userId, String courseId) {
        System.out.println("Service: getEnrollmentStatus called for userId=" + userId + ", courseId=" + courseId);
        
        Optional<CourseProgress> result = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (result.isPresent()) {
            CourseProgress progress = result.get();
            System.out.println("Service: Found enrollment - Progress: " + progress.getProgressPercentage() + 
                             "%, Completed lessons: " + (progress.getCompletedLessons() != null ? progress.getCompletedLessons().size() : 0) +
                             ", Started: " + progress.getStartedAt());
        } else {
            System.out.println("Service: No enrollment found for user " + userId + " in course " + courseId);
        }
        
        return result;
    }

    public CourseProgress enrollUserInCourse(String userId, String courseId) {
        // Check if already enrolled
        Optional<CourseProgress> existing = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new enrollment
        CourseProgress progress = new CourseProgress();
        progress.setUserId(userId);
        progress.setCourseId(courseId);
        progress.setEnrolledAt(new Date());
        progress.setCompleted(false);
        progress.setProgressPercentage(0);

        CourseProgress saved = courseProgressRepository.save(progress);
        activityLogService.logUserActivity(
                userId,
                ActivityType.COURSE_ENROLLMENT,
                "Enrolled in course",
                "COURSE",
                courseId,
                Map.of("courseId", courseId)
        );
        return saved;
    }

    public List<CourseProgress> getUserEnrollments(String userId) {
        return courseProgressRepository.findByUserId(userId);
    }

    public List<CourseProgress> getCourseEnrollments(String courseId) {
        return courseProgressRepository.findByCourseId(courseId);
    }

    public CourseProgress updateProgress(String userId, String courseId, int progressPercentage) {
        Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (progressOpt.isPresent()) {
            CourseProgress progress = progressOpt.get();
            boolean wasCompleted = progress.isCompleted();
            progress.setProgressPercentage(progressPercentage);

            if (progressPercentage >= 100) {
                progress.setCompleted(true);
            }

            CourseProgress saved = courseProgressRepository.save(progress);
            if (!wasCompleted && saved.isCompleted()) {
                activityLogService.logUserActivity(
                        userId,
                        ActivityType.COURSE_COMPLETION,
                        "Completed course",
                        "COURSE",
                        courseId,
                        Map.of("courseId", courseId)
                );
            }
            return saved;
        }
        
        throw new RuntimeException("User is not enrolled in this course");
    }

    public void unenrollUser(String userId, String courseId) {
        Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        progressOpt.ifPresent(courseProgressRepository::delete);
    }

    public CourseProgress markLessonComplete(String userId, String courseId, String lessonId, Date completedAt) {
        System.out.println("Service: markLessonComplete called with userId=" + userId + 
                          ", courseId=" + courseId + ", lessonId=" + lessonId);
        
        Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (progressOpt.isPresent()) {
            CourseProgress progress = progressOpt.get();
            
            System.out.println("Service: Found existing progress. Current completed lessons: " + 
                             progress.getCompletedLessons());
            
            // Ensure completedLessons list is initialized
            if (progress.getCompletedLessons() == null) {
                progress.setCompletedLessons(new ArrayList<>());
                System.out.println("Service: Initialized empty completedLessons list");
            }
            
            // Ensure lessonCompletionDates map is initialized
            if (progress.getLessonCompletionDates() == null) {
                progress.setLessonCompletionDates(new HashMap<>());
                System.out.println("Service: Initialized empty lessonCompletionDates map");
            }
            
            // Add lesson to completed list if not already completed
            if (!progress.getCompletedLessons().contains(lessonId)) {
                progress.getCompletedLessons().add(lessonId);
                progress.getLessonCompletionDates().put(lessonId, completedAt != null ? completedAt : new Date());
                System.out.println("Service: Added lesson " + lessonId + " to completed list");
            } else {
                System.out.println("Service: Lesson " + lessonId + " was already completed");
            }
            
            // Set startedAt if this is the first lesson completion
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(new Date());
                System.out.println("Service: Set startedAt timestamp");
            }
            
            System.out.println("Service: Saving progress with completed lessons: " + 
                             progress.getCompletedLessons());
            
            CourseProgress savedProgress = courseProgressRepository.save(progress);
            
            System.out.println("Service: Saved progress. Final completed lessons: " + 
                             savedProgress.getCompletedLessons());
            
            return savedProgress;
        }
        
        System.err.println("Service: User " + userId + " is not enrolled in course " + courseId);
        throw new RuntimeException("User is not enrolled in this course");
    }

    public CourseProgress setCurrentLesson(String userId, String courseId, String lessonId) {
        Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (progressOpt.isPresent()) {
            CourseProgress progress = progressOpt.get();
            progress.setCurrentLessonId(lessonId);
            
            // Set startedAt if this is the first lesson interaction
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(new Date());
            }
            
            return courseProgressRepository.save(progress);
        }
        
        throw new RuntimeException("User is not enrolled in this course");
    }

    public CourseProgress markCourseComplete(String userId, String courseId, Date completedAt) {
        Optional<CourseProgress> progressOpt = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        
        if (progressOpt.isPresent()) {
            CourseProgress progress = progressOpt.get();
            boolean wasCompleted = progress.isCompleted();
            progress.setCompleted(true);
            progress.setProgressPercentage(100);
            
            // Set startedAt if not already set
            if (progress.getStartedAt() == null) {
                progress.setStartedAt(new Date());
            }
            
            // Generate certificate URL (placeholder for now)
            if (progress.getCertificateUrl() == null || progress.getCertificateUrl().isEmpty()) {
                progress.setCertificateUrl("https://certificates.agra.com/course/" + courseId + "/user/" + userId);
            }
            
            CourseProgress saved = courseProgressRepository.save(progress);
            if (!wasCompleted) {
                activityLogService.logUserActivity(
                        userId,
                        ActivityType.COURSE_COMPLETION,
                        "Completed course",
                        "COURSE",
                        courseId,
                        Map.of("courseId", courseId)
                );
            }
            return saved;
        }
        
        throw new RuntimeException("User is not enrolled in this course");
    }

    /**
     * Cleanup method to remove orphaned enrollments (enrollments for deleted courses)
     * This should be called periodically or after course deletions
     */
    public int cleanupOrphanedEnrollments(CourseService courseService) {
        System.out.println("CourseProgressService: Starting cleanup of orphaned enrollments");
        
        List<CourseProgress> allEnrollments = courseProgressRepository.findAll();
        int deletedCount = 0;
        
        for (CourseProgress progress : allEnrollments) {
            try {
                // Check if the course still exists
                if (!courseService.getCourseById(progress.getCourseId()).isPresent()) {
                    System.out.println("CourseProgressService: Found orphaned enrollment - CourseId: " + 
                                     progress.getCourseId() + ", UserId: " + progress.getUserId());
                    courseProgressRepository.delete(progress);
                    deletedCount++;
                }
            } catch (Exception e) {
                System.err.println("CourseProgressService: Error checking course " + progress.getCourseId() + 
                                 ": " + e.getMessage());
            }
        }
        
        System.out.println("CourseProgressService: Cleanup completed. Deleted " + deletedCount + " orphaned enrollments");
        return deletedCount;
    }
}
