package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.CertificateRecordRepository;
import org.agra.agra_backend.dao.CourseProgressRepository;
import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.CertificateRecord;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
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
                .map(Course::getTitle)
                .orElse(null);
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
