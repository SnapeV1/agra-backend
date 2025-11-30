package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.LikeRepository;
import org.agra.agra_backend.model.Like;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseLikeService {
    public static final String TARGET_TYPE_COURSE = "COURSE";

    private final LikeRepository likeRepository;
    private final CourseRepository courseRepository;

    public CourseLikeService(LikeRepository likeRepository, CourseRepository courseRepository) {
        this.likeRepository = likeRepository;
        this.courseRepository = courseRepository;
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