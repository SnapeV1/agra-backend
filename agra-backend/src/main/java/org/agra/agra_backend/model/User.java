package org.agra.agra_backend.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

import org.agra.agra_backend.model.NotificationPreferences;

@AllArgsConstructor
@NoArgsConstructor
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
    private String themePreference = "light";
    private Date registeredAt;
    private NotificationPreferences notificationPreferences;
    private Boolean isArchived=false;
    private Boolean verified = false;

    // Admin / security controls
    private Boolean twoFactorEnabled = false;
    private String twoFactorSecret;
    private List<String> twoFactorRecoveryCodes;
    private Date twoFactorVerifiedAt;

    public User(String id, String name, String picture) {
    }
}
