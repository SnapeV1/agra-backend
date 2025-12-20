package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.CertificateRecord;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CertificateService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class CertificateController {
    private static final String KEY_VALID = "valid";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String MSG_AUTH_REQUIRED = "Authentication required";
    private static final String KEY_INSTRUCTOR_NAME = "instructorName";
    private static final String KEY_ORGANIZATION_NAME = "organizationName";
    private static final String KEY_COURSE_ID = "courseId";
    private static final String KEY_COURSE_TITLE = "courseTitle";
    private final CourseProgressService courseProgressService;
    private final CertificateService certificateService;
    private final CourseService courseService;

    public CertificateController(CourseProgressService courseProgressService,
                                 CertificateService certificateService,
                                 CourseService courseService) {
        this.courseProgressService = courseProgressService;
        this.certificateService = certificateService;
        this.courseService = courseService;
    }

    @GetMapping("/validate/{certificateCode}")
    public ResponseEntity<Map<String, Object>> validateCertificate(@PathVariable String certificateCode) {
        return certificateService.findByCode(certificateCode)
                .map(this::buildSuccessResponse)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                KEY_VALID, false,
                                KEY_MESSAGE, "Certificate not found or invalid"
                        )));
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<Map<String, Object>> verifyCertificate(@PathVariable String certificateId) {
        return certificateService.verifyCertificate(certificateId)
                .map(payload -> {
                    applyLocalizedCourseFields(payload);
                    boolean valid = Boolean.TRUE.equals(payload.get(KEY_VALID));
                    HttpStatus status = valid ? HttpStatus.OK : HttpStatus.GONE;
                    return ResponseEntity.status(status).body(payload);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_VALID, false, KEY_MESSAGE, "Certificate not found")));
    }

    @GetMapping("/user/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getCertificateForAuthenticatedUser(
            @PathVariable String courseId,
            Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
        }

        User requester = (User) authentication.getPrincipal();
        String userId = requester.getId();

        return certificateService.findByCourseAndUser(courseId, userId)
                .map(this::buildSuccessResponse)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(KEY_ERROR, "Certificate not found")));
    }

    @GetMapping("/admin/issued")
    public ResponseEntity<?> listIssuedCertificates(Authentication authentication) {
        ResponseEntity<Map<String, Object>> authError = requireAdmin(authentication);
        if (authError != null) {
            return ResponseEntity.status(authError.getStatusCode()).body(authError.getBody());
        }
        List<Map<String, Object>> data = certificateService.getAllCertificates().stream()
                .map(this::mapCertificate)
                .toList();
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{certificateId}")
    public ResponseEntity<Map<String, Object>> updateCertificate(
            @PathVariable String certificateId,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        ResponseEntity<Map<String, Object>> authError = requireAdmin(authentication);
        if (authError != null) {
            return authError;
        }
        try {
            Date issueDate = parseDate(updates.getOrDefault("issueDate", updates.get("issuedAt")));
            Date completionDate = parseDate(updates.getOrDefault("completionDate", updates.get("completedAt")));
            String instructorName = (String) updates.get("instructorName");
            String organizationName = (String) updates.get("organizationName");
            String notes = (String) updates.get("notes");

            CertificateRecord updated = certificateService.updateMetadata(
                    certificateId,
                    instructorName,
                    organizationName,
                    issueDate,
                    completionDate,
                    notes);

            return buildSuccessResponse(updated);
        } catch (IllegalArgumentException | ParseException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_ERROR, ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to update certificate"));
        }
    }

    @PostMapping("/{certificateId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeCertificate(
            @PathVariable String certificateId,
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        ResponseEntity<Map<String, Object>> authError = requireAdmin(authentication);
        if (authError != null) {
            return authError;
        }
        try {
            String reason = payload != null ? (String) payload.get("reason") : null;
            CertificateRecord certificateRecord = certificateService.revokeCertificate(certificateId, reason);
            return buildSuccessResponse(certificateRecord);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(KEY_ERROR, ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to revoke certificate"));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication authentication) {
        ResponseEntity<Map<String, Object>> authError = requireAdmin(authentication);
        if (authError != null) {
            return ResponseEntity.status(authError.getStatusCode()).body(authError.getBody());
        }
        return ResponseEntity.ok(certificateService.getStatistics());
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(CertificateRecord certificateRecord) {
        Map<String, Object> response = mapCertificate(certificateRecord);
        HttpStatus status = certificateRecord.isRevoked() ? HttpStatus.GONE : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    private Map<String, Object> mapCertificate(CertificateRecord certificateRecord) {
        Map<String, Object> response = new HashMap<>();
        boolean isValid = !certificateRecord.isRevoked();
        response.put(KEY_VALID, isValid);
        response.put("isValid", isValid);
        response.put("certificateId", certificateRecord.getId());
        response.put("certificateCode", certificateRecord.getCertificateCode());
        response.put("verificationCode", certificateRecord.getCertificateCode());
        response.put("verificationUrl", buildVerificationUrl(certificateRecord.getCertificateCode()));
        response.put("id", certificateRecord.getId());
        response.put("userId", certificateRecord.getUserId());
        response.put("studentId", certificateRecord.getUserId());
        response.put("recipientName", certificateRecord.getRecipientName());
        response.put("studentName", certificateRecord.getRecipientName());
        response.put(KEY_COURSE_ID, certificateRecord.getCourseId());
        response.put(KEY_COURSE_TITLE, certificateRecord.getCourseTitle());
        response.put("certificateUrl", certificateRecord.getCertificateUrl());
        response.put("issuedAt", certificateRecord.getIssuedAt());
        response.put("issueDate", certificateRecord.getIssuedAt());
        response.put("completedAt", certificateRecord.getCompletedAt());
        response.put("completionDate", certificateRecord.getCompletedAt());
        response.put("revoked", certificateRecord.isRevoked());
        response.put("revokedReason", certificateRecord.getRevokedReason());
        response.put("revokedAt", certificateRecord.getRevokedAt());
        response.put(KEY_ORGANIZATION_NAME, certificateRecord.getOrganizationName());
        response.put(KEY_INSTRUCTOR_NAME, certificateRecord.getInstructorName());
        response.put("notes", certificateRecord.getNotes());
        response.put("certificateProgressId", certificateRecord.getCourseProgressId());
        response.put("totalTimeSpent", null);

        courseProgressService.getEnrollmentStatus(certificateRecord.getUserId(), certificateRecord.getCourseId())
                .ifPresent(progress -> {
                    response.put("completionPercentage", progress.getProgressPercentage());
                    response.put("totalLessons", progress.getCompletedLessons() != null ? progress.getCompletedLessons().size() : null);
                });

        courseService.getCourseById(certificateRecord.getCourseId())
                .map(course -> courseService.localizeCourse(course, LocaleContextHolder.getLocale()))
                .ifPresent(course -> {
                    response.put("courseCountry", course.getCountry());
                    response.put("courseDomain", course.getDomain());
                    response.put("totalLessons", calculateTotalLessons(course));
                    response.put(KEY_COURSE_TITLE, course.getTitle());
                });

        if (response.get(KEY_ORGANIZATION_NAME) == null) {
            response.put(KEY_ORGANIZATION_NAME, "YEFFA Learning Platform");
        }
        if (response.get(KEY_INSTRUCTOR_NAME) == null) {
            response.put(KEY_INSTRUCTOR_NAME, "AGRA Trainer");
        }

        return response;
    }

    private Integer calculateTotalLessons(Course course) {
        if (course == null) {
            return null;
        }
        int sessionCount = course.getSessionIds() != null ? course.getSessionIds().size() : 0;
        int textCount = course.getTextContent() != null ? course.getTextContent().size() : 0;
        return sessionCount + textCount;
    }

    private void applyLocalizedCourseFields(Map<String, Object> payload) {
        if (payload == null) return;
        Object courseIdObj = payload.get(KEY_COURSE_ID);
        if (!(courseIdObj instanceof String courseId) || courseId.isBlank()) {
            return;
        }
        courseService.getCourseById(courseId)
                .map(course -> courseService.localizeCourse(course, LocaleContextHolder.getLocale()))
                .ifPresent(course -> {
                    payload.put(KEY_COURSE_TITLE, course.getTitle());
                    Object courseObj = payload.get("course");
                    if (courseObj instanceof Map<?, ?> courseMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) courseMap;
                        typed.put("title", course.getTitle());
                        typed.put("description", course.getDescription());
                    }
                });
    }

    private String buildVerificationUrl(String code) {
        if (!org.springframework.util.StringUtils.hasText(code)) {
            return null;
        }
        return "https://certificates.agra.com/verify/" + code;
    }

    private ResponseEntity<Map<String, Object>> requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
        }
        User user = (User) authentication.getPrincipal();
        if (!isAdmin(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(KEY_ERROR, "Admin privileges required"));
        }
        return null;
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().equalsIgnoreCase("ADMIN");
    }

    private Date parseDate(Object value) throws ParseException {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new Date(number.longValue());
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Date.from(java.time.Instant.parse(str));
            } catch (java.time.format.DateTimeParseException ignored) {
                SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    return isoFormatter.parse(str);
                } catch (ParseException e) {
                    SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd");
                    dateOnly.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return dateOnly.parse(str);
                }
            }
        }
        throw new ParseException("Unsupported date value", 0);
    }

    @PostMapping("/generate/{courseId}")
    public ResponseEntity<?> generateCertificate(
            @PathVariable String courseId,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(KEY_ERROR, MSG_AUTH_REQUIRED));
            }

            User user = (User) authentication.getPrincipal();
            String userId = user.getId();

            return courseProgressService.getEnrollmentStatus(userId, courseId)
                    .map(progress -> {
                        if (progress.isCompleted()) {
                            // Generate or update certificate URL
                            String certificateUrl = "https://certificates.agra.com/course/" + courseId + "/user/" + userId + "/certificate.pdf";
                            progress.setCertificateUrl(certificateUrl);
                            CertificateRecord certificateRecord = certificateService.recordIssuance(progress, certificateUrl, null);

                            return ResponseEntity.ok(Map.of(
                                    KEY_MESSAGE, "Certificate generated successfully",
                                    "certificateUrl", certificateRecord.getCertificateUrl(),
                                    "certificateCode", certificateRecord.getCertificateCode(),
                                    "certificateIssuedAt", certificateRecord.getIssuedAt(),
                                    KEY_COURSE_ID, courseId,
                                    "userId", userId
                            ));
                        } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of(KEY_ERROR, "Course must be completed before generating certificate"));
                        }
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(KEY_ERROR, "User is not enrolled in this course")));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(KEY_ERROR, "Failed to generate certificate"));
        }
    }
}
