package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.model.ActivityType;
import org.agra.agra_backend.model.Like;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseLikeService {
    public static final String TARGET_TYPE_COURSE = "COURSE";

    private final LikeRepository likeRepository;
    private final CourseRepository courseRepository;
    private final ActivityLogService activityLogService;

    public CourseLikeService(LikeRepository likeRepository,
                             CourseRepository courseRepository,
                             ActivityLogService activityLogService) {
        this.likeRepository = likeRepository;
        this.courseRepository = courseRepository;
        this.activityLogService = activityLogService;
    }

    public boolean likeCourse(String userId, String courseId) {
        // Ensure course exists
        if (courseRepository.findById(courseId).isEmpty()) {
            return false;
        }
        if (likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COURSE, courseId)) {
            return true; // already liked, idempotent
        }
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType(TARGET_TYPE_COURSE);
        like.setTargetId(courseId);
        likeRepository.save(like);
        if (activityLogService != null) {
            activityLogService.logUserActivity(
                    userId,
                    ActivityType.LIKE,
                    "Liked course",
                    TARGET_TYPE_COURSE,
                    courseId,
                    Map.of("courseId", courseId)
            );
        }
        return true;
    }

    public boolean unlikeCourse(String userId, String courseId) {
        // Ensure course exists
        if (courseRepository.findById(courseId).isEmpty()) {
            return false;
        }
        likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COURSE, courseId);
        return true;
    }

    public boolean isLiked(String userId, String courseId) {
        return likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COURSE, courseId);
    }

    public List<String> listLikedCourseIds(String userId) {
        return likeRepository.findByUserId(userId)
                .stream()
                .filter(l -> TARGET_TYPE_COURSE.equals(l.getTargetType()))
                .map(Like::getTargetId)
                .collect(Collectors.toList());
    }
}
