package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Getter
@Setter
@Document(collection = "course_progress")
public class CourseProgress {
    @Id
    private String id;
    private String userId;
    private String courseId;
    private Date enrolledAt;
    private Date startedAt;
    private boolean completed;
    private String certificateUrl;
    private String certificateCode;
    private Date certificateIssuedAt;
    private int progressPercentage = 0;
    
    // Lesson tracking fields
    private String currentLessonId;
    private List<String> completedLessons = new ArrayList<>();
    private Map<String, Date> lessonCompletionDates = new HashMap<>();
}
