package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CourseProgress {
    private String courseId;
    private Date startedAt;
    private boolean completed;
    private String certificateUrl;
}
