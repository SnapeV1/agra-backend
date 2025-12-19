package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseTranslation {
    private String title;
    private String description;
    private List<String> goals;
}
