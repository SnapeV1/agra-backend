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
    private List<String> goals;
    private String domain;
    private String country;
    private String trainerId;
    private List<String> sessionIds;
    private List<String> languagesAvailable;
    private Date createdAt;
    private Date updatedAt;
    private boolean archived = false;


    // Cloudinary image fields
    private String imagePublicId;  // Store Cloudinary public_id
    private String imageUrl;       // Generated optimized URL for course cards
    private String thumbnailUrl;   // Generated thumbnail URL
    private String detailImageUrl; // Generated high-res URL for course details



    private String videoUrl;
    private String videoPublicId;
    private List<CourseFile> files;
    private List<TextContent> textContent;
}
