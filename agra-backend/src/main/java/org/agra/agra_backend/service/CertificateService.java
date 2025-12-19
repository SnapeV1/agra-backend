package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CertificateRecordRepository;
import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.CertificateRecord;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.CourseTranslation;
import org.agra.agra_backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    private final CertificateRecordRepository certificateRecordRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CertificateService(CertificateRecordRepository certificateRecordRepository,
                              CourseProgressRepository courseProgressRepository,
                              UserRepository userRepository,
                              CourseRepository courseRepository) {
        this.certificateRecordRepository = certificateRecordRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public List<CertificateRecord> getAllCertificates() {
        return certificateRecordRepository.findAll();
    }

    public Optional<CertificateRecord> getById(String certificateId) {
        if (!StringUtils.hasText(certificateId)) {
            return Optional.empty();
        }
        return certificateRecordRepository.findById(certificateId);
    }

    public CertificateRecord updateMetadata(String certificateId,
                                            String instructorName,
                                            String organizationName,
                                            Date issuedAt,
                                            Date completedAt,
                                            String notes) {

        CertificateRecord record = getById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        if (StringUtils.hasText(instructorName)) {
            record.setInstructorName(instructorName);
        }
        if (StringUtils.hasText(organizationName)) {
            record.setOrganizationName(organizationName);
        }
        if (issuedAt != null) {
            record.setIssuedAt(issuedAt);
        }
        if (completedAt != null) {
            record.setCompletedAt(completedAt);
        }
        if (notes != null) {
            record.setNotes(notes);
        }

        return certificateRecordRepository.save(record);
    }

    public CertificateRecord revokeCertificate(String certificateId, String reason) {
        CertificateRecord record = getById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        record.setRevoked(true);
        record.setRevokedReason(reason);
        record.setRevokedAt(new Date());

        return certificateRecordRepository.save(record);
    }

    public Map<String, Object> getStatistics() {
        long totalIssued = certificateRecordRepository.count();
        long totalRevoked = certificateRecordRepository.countByRevoked(true);
        long active = totalIssued - totalRevoked;

        Map<String, Long> perCourse = certificateRecordRepository.findAll().stream()
                .collect(Collectors.groupingBy(CertificateRecord::getCourseId, Collectors.counting()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssued", totalIssued);
        stats.put("totalRevoked", totalRevoked);
        stats.put("active", active);
        stats.put("byCourse", perCourse);
        return stats;
    }

    /**
     * Records (or updates) a certificate issuance for the provided course progress entry.
     * @param progress the completed course progress document
     * @param certificateUrl the URL of the generated certificate
     * @param completedAt optional completion timestamp supplied by the caller
     * @return the persisted certificate record
     */
    public CertificateRecord recordIssuance(CourseProgress progress,
                                            String certificateUrl,
                                            Date completedAt) {
        if (progress == null) {
            throw new IllegalArgumentException("Course progress is required to issue a certificate");
        }

        String certificateCode = ensureCertificateCode(progress);
        Date issuedAt = ensureIssuedAt(progress);

        String resolvedUrl = resolveCertificateUrl(progress, certificateUrl);
        progress.setCertificateUrl(resolvedUrl);

        courseProgressRepository.save(progress);

        CertificateRecord record = certificateRecordRepository
                .findByCourseIdAndUserId(progress.getCourseId(), progress.getUserId())
                .orElse(new CertificateRecord());

        record.setCertificateCode(certificateCode);
        record.setCertificateUrl(resolvedUrl);
        record.setCourseId(progress.getCourseId());
        record.setCourseProgressId(progress.getId());
        record.setUserId(progress.getUserId());
        record.setCompletedAt(resolveCompletionDate(progress, completedAt, record.getCompletedAt()));
        record.setIssuedAt(issuedAt);
        record.setRecipientName(resolveUserName(progress.getUserId()));
        record.setCourseTitle(resolveCourseTitle(progress.getCourseId()));
        record.setRevoked(false);

        return certificateRecordRepository.save(record);
    }

    public Optional<CertificateRecord> findByCode(String certificateCode) {
        if (!StringUtils.hasText(certificateCode)) {
            return Optional.empty();
        }
        String normalized = certificateCode.trim().toUpperCase();
        return certificateRecordRepository.findByCertificateCode(normalized);
    }

    public Optional<CertificateRecord> findByCourseAndUser(String courseId, String userId) {
        if (!StringUtils.hasText(courseId) || !StringUtils.hasText(userId)) {
            return Optional.empty();
        }
        return certificateRecordRepository.findByCourseIdAndUserId(courseId, userId);
    }

    /**
     * Verifies a certificate by its public code or internal id and returns enriched details
     * about the recipient and course when valid.
     */
    public Optional<Map<String, Object>> verifyCertificate(String certificateIdentifier) {
        if (!StringUtils.hasText(certificateIdentifier)) {
            return Optional.empty();
        }
        String normalized = certificateIdentifier.trim();

        Optional<CertificateRecord> recordOpt = findByCode(normalized);
        if (recordOpt.isEmpty()) {
            recordOpt = getById(normalized);
        }
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        CertificateRecord record = recordOpt.get();
        Map<String, Object> response = buildVerificationPayload(record);
        if (record.isRevoked()) {
            response.put("valid", false);
            response.put("message", record.getRevokedReason() != null ? record.getRevokedReason() : "Certificate revoked");
        } else {
            response.put("valid", true);
        }
        return Optional.of(response);
    }

    private Map<String, Object> buildVerificationPayload(CertificateRecord record) {
        Map<String, Object> response = new HashMap<>();
        response.put("certificateId", record.getId());
        response.put("certificateCode", record.getCertificateCode());
        response.put("certificateUrl", record.getCertificateUrl());
        response.put("issuedAt", record.getIssuedAt());
        response.put("completedAt", record.getCompletedAt());
        response.put("revoked", record.isRevoked());
        response.put("revokedReason", record.getRevokedReason());

        userRepository.findById(record.getUserId()).ifPresent(user -> {
            response.put("userId", user.getId());
            response.put("recipientName", user.getName());
            response.put("userName", user.getName());
            response.put("email", user.getEmail());
            response.put("birthdate", user.getBirthdate());
        });

        courseRepository.findById(record.getCourseId()).ifPresent(course -> {
            Map<String, Object> courseDetails = new HashMap<>();
            courseDetails.put("id", course.getId());
            courseDetails.put("title", resolveCourseTitle(course));
            courseDetails.put("description", resolveCourseDescription(course));
            courseDetails.put("domain", course.getDomain());
            courseDetails.put("country", course.getCountry());
            courseDetails.put("trainerId", course.getTrainerId());
            response.put("course", courseDetails);
            response.put("courseId", course.getId());
            response.put("courseTitle", resolveCourseTitle(course));
        });

        return response;
    }

    private String ensureCertificateCode(CourseProgress progress) {
        if (StringUtils.hasText(progress.getCertificateCode())) {
            String normalized = progress.getCertificateCode().trim().toUpperCase();
            progress.setCertificateCode(normalized);
            return normalized;
        }

        String code;
        do {
            code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        } while (certificateRecordRepository.findByCertificateCode(code).isPresent());

        progress.setCertificateCode(code);
        return code;
    }

    private Date ensureIssuedAt(CourseProgress progress) {
        if (progress.getCertificateIssuedAt() != null) {
            return progress.getCertificateIssuedAt();
        }
        Date now = new Date();
        progress.setCertificateIssuedAt(now);
        return now;
    }

    private String resolveUserName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse(null);
    }

    private String resolveCourseTitle(String courseId) {
        if (!StringUtils.hasText(courseId)) {
            return null;
        }
        return courseRepository.findById(courseId)
                .map(this::resolveCourseTitle)
                .orElse(null);
    }

    private String resolveCourseTitle(Course course) {
        if (course == null) return null;
        String title = course.getTitle();
        if (StringUtils.hasText(title)) return title;
        CourseTranslation translation = resolveFallbackTranslation(course);
        if (translation != null && StringUtils.hasText(translation.getTitle())) {
            return translation.getTitle();
        }
        return title;
    }

    private String resolveCourseDescription(Course course) {
        if (course == null) return null;
        String description = course.getDescription();
        if (StringUtils.hasText(description)) return description;
        CourseTranslation translation = resolveFallbackTranslation(course);
        if (translation != null && StringUtils.hasText(translation.getDescription())) {
            return translation.getDescription();
        }
        return description;
    }

    private CourseTranslation resolveFallbackTranslation(Course course) {
        if (course == null || course.getTranslations() == null || course.getTranslations().isEmpty()) return null;
        String defaultLanguage = course.getDefaultLanguage();
        if (StringUtils.hasText(defaultLanguage)) {
            CourseTranslation preferred = course.getTranslations().get(defaultLanguage);
            if (preferred != null) return preferred;
        }
        CourseTranslation en = course.getTranslations().get("en");
        if (en != null) return en;
        return course.getTranslations().values().stream().findFirst().orElse(null);
    }

    private String resolveCertificateUrl(CourseProgress progress, String overrideUrl) {
        if (StringUtils.hasText(overrideUrl)) {
            return overrideUrl;
        }
        if (StringUtils.hasText(progress.getCertificateUrl())) {
            return progress.getCertificateUrl();
        }
        if (!StringUtils.hasText(progress.getCertificateCode())) {
            return null;
        }
        return "https://certificates.agra.com/course/" + progress.getCourseId()
                + "/user/" + progress.getUserId()
                + "/certificate-" + progress.getCertificateCode() + ".pdf";
    }

    private Date resolveCompletionDate(CourseProgress progress, Date preferred, Date existingRecordDate) {
        if (preferred != null) {
            return preferred;
        }
        if (existingRecordDate != null) {
            return existingRecordDate;
        }

        Date candidate = null;
        if (progress.getLessonCompletionDates() != null) {
            for (Date date : progress.getLessonCompletionDates().values()) {
                if (date == null) {
                    continue;
                }
                if (candidate == null || date.after(candidate)) {
                    candidate = date;
                }
            }
        }
        if (candidate == null) {
            candidate = progress.getStartedAt();
        }
        if (candidate == null) {
            candidate = progress.getEnrolledAt();
        }
        return candidate;
    }
}
