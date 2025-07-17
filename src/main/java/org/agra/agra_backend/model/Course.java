package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document(collection = "courses")
public class Course {
    @Id
    private String id;

    private String title;
    private String description;
    private String domain;
    private String trainerId;
    private List<String> languagesAvailable;
    private Date createdAt;
    private Date updatedAt;
}
