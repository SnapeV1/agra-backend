package org.agra.agra_backend.model;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String name;
    private String email;
    private String phone;
    private String password;
    private String country;
    private String language;
    private String domain;
    private String role;
    private String picture;
    private Date registeredAt;
    private List<CourseProgress> progress;
}
