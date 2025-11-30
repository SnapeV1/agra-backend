package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "training_kits")
public class TrainingKit {
    @Id
    private String id;

    private String courseId;
    private String title;
    private String description;
    private String fileUrl;
    private String uploadedBy;
    private Date uploadDate;
    private String language;
}
