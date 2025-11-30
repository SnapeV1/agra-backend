package org.agra.agra_backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "certificates")
public class CertificateRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String certificateCode;

    @Indexed
    private String userId;

    @Indexed
    private String courseId;

    private String courseProgressId;
    private String recipientName;
    private String courseTitle;
    private String certificateUrl;
    private Date issuedAt;
    private Date completedAt;
    private boolean revoked = false;
    private String instructorName;
    private String organizationName;
    private String notes;
    private String revokedReason;
    private Date revokedAt;
}
