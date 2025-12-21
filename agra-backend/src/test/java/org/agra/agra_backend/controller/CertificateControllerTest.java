package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.CertificateRecord;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.TextContent;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CertificateService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateControllerTest {

    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private CertificateController controller;

    private Authentication authWithRole(String userId, String role) {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }

    private CertificateRecord sampleRecord(String code) {
        CertificateRecord certificateRecord = new CertificateRecord();
        certificateRecord.setId("cert-1");
        certificateRecord.setCertificateCode(code);
        certificateRecord.setUserId("user-1");
        certificateRecord.setCourseId("course-1");
        certificateRecord.setRecipientName("Student");
        certificateRecord.setCourseTitle("Course");
        certificateRecord.setCertificateUrl("https://certs/test");
        certificateRecord.setIssuedAt(new Date());
        certificateRecord.setCompletedAt(new Date());
        return certificateRecord;
    }

    private Course sampleCourse() {
        Course course = new Course();
        course.setId("course-1");
        course.setTitle("Localized Course");
        course.setDescription("Localized Description");
        course.setCountry("GH");
        course.setDomain("Agriculture");
        course.setSessionIds(List.of("s1", "s2"));
        course.setTextContent(List.of(new TextContent(), new TextContent(), new TextContent()));
        return course;
    }

    private CourseProgress sampleProgress() {
        CourseProgress progress = new CourseProgress();
        progress.setUserId("user-1");
        progress.setCourseId("course-1");
        progress.setProgressPercentage(80);
        progress.setCompletedLessons(new ArrayList<>(List.of("l1", "l2")));
        return progress;
    }

    @Test
    void validateCertificateReturnsNotFound() {
        when(certificateService.findByCode("code")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.validateCertificate("code");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("valid", false);
    }

    @Test
    void validateCertificateReturnsOkWhenActive() {
        CertificateRecord certificateRecord = sampleRecord("CODE-1");
        when(certificateService.findByCode("CODE-1")).thenReturn(Optional.of(certificateRecord));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.validateCertificate("CODE-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("valid", true)
                .containsEntry("verificationUrl", "https://certificates.agra.com/verify/CODE-1")
                .containsEntry("organizationName", "YEFFA Learning Platform")
                .containsEntry("instructorName", "AGRA Trainer");
    }

    @Test
    void validateCertificateReturnsGoneWhenRevoked() {
        CertificateRecord certificateRecord = sampleRecord("CODE-2");
        certificateRecord.setRevoked(true);
        when(certificateService.findByCode("CODE-2")).thenReturn(Optional.of(certificateRecord));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.validateCertificate("CODE-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void verifyCertificateReturnsLocalizedResponse() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("valid", true);
        payload.put("courseId", "course-1");
        payload.put("course", new HashMap<>(Map.of("title", "Old", "description", "Old")));
        when(certificateService.verifyCertificate("cert-1")).thenReturn(Optional.of(payload));

        Course course = sampleCourse();
        when(courseService.getCourseById("course-1")).thenReturn(Optional.of(course));
        when(courseService.localizeCourse(
                org.mockito.ArgumentMatchers.eq(course),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(course);

        ResponseEntity<Map<String, Object>> response = controller.verifyCertificate("cert-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("courseTitle", "Localized Course");
        @SuppressWarnings("unchecked")
        Map<String, Object> courseMap = (Map<String, Object>) response.getBody().get("course");
        assertThat(courseMap)
                .containsEntry("title", "Localized Course")
                .containsEntry("description", "Localized Description");
    }

    @Test
    void verifyCertificateReturnsGoneWhenInvalid() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("valid", false);
        when(certificateService.verifyCertificate("cert-2")).thenReturn(Optional.of(payload));

        ResponseEntity<Map<String, Object>> response = controller.verifyCertificate("cert-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void verifyCertificateReturnsNotFound() {
        when(certificateService.verifyCertificate("missing")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.verifyCertificate("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCertificateForAuthenticatedUserReturnsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = controller.getCertificateForAuthenticatedUser("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCertificateForAuthenticatedUserReturnsNotFound() {
        Authentication authentication = authWithRole("user-1", "USER");
        when(certificateService.findByCourseAndUser("course-1", "user-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getCertificateForAuthenticatedUser("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCertificateForAuthenticatedUserReturnsSuccess() {
        Authentication authentication = authWithRole("user-1", "USER");
        CertificateRecord certificateRecord = sampleRecord("CODE-3");
        when(certificateService.findByCourseAndUser("course-1", "user-1")).thenReturn(Optional.of(certificateRecord));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(sampleProgress()));
        Course course = sampleCourse();
        when(courseService.getCourseById("course-1")).thenReturn(Optional.of(course));
        when(courseService.localizeCourse(
                org.mockito.ArgumentMatchers.eq(course),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(course);

        ResponseEntity<Map<String, Object>> response = controller.getCertificateForAuthenticatedUser("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("certificateCode", "CODE-3");
        assertThat(response.getBody()).containsEntry("courseTitle", "Localized Course");
    }

    @Test
    void listIssuedCertificatesReturnsUnauthorized() {
        ResponseEntity<?> response = controller.listIssuedCertificates(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listIssuedCertificatesReturnsForbidden() {
        ResponseEntity<?> response = controller.listIssuedCertificates(authWithRole("user-1", "USER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listIssuedCertificatesReturnsData() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        CertificateRecord certificateRecord = sampleRecord("CODE-4");
        when(certificateService.getAllCertificates()).thenReturn(List.of(certificateRecord));
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.listIssuedCertificates(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(List.class);
    }

    @Test
    void updateCertificateReturnsBadRequestOnParse() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        Map<String, Object> updates = Map.of("issueDate", "bad-date");

        ResponseEntity<Map<String, Object>> response = controller.updateCertificate("cert-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateCertificateReturnsBadRequestOnServiceError() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        Map<String, Object> updates = Map.of("issueDate", 123L);
        when(certificateService.updateMetadata("cert-1", null, null, new Date(123L), null, null))
                .thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<Map<String, Object>> response = controller.updateCertificate("cert-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateCertificateReturnsSuccess() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        Map<String, Object> updates = Map.of(
                "issueDate", "2025-01-01T00:00:00.000Z",
                "completionDate", "2025-01-02",
                "instructorName", "Trainer",
                "organizationName", "Org",
                "notes", "Updated"
        );
        CertificateRecord certificateRecord = sampleRecord("CODE-5");
        when(certificateService.updateMetadata(
                org.mockito.ArgumentMatchers.eq("cert-1"),
                org.mockito.ArgumentMatchers.eq("Trainer"),
                org.mockito.ArgumentMatchers.eq("Org"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("Updated")
        )).thenReturn(certificateRecord);

        ResponseEntity<Map<String, Object>> response = controller.updateCertificate("cert-1", updates, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("certificateCode", "CODE-5");
    }

    @Test
    void revokeCertificateReturnsSuccess() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        CertificateRecord certificateRecord = sampleRecord("CODE-6");
        when(certificateService.revokeCertificate("cert-1", null)).thenReturn(certificateRecord);
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.revokeCertificate("cert-1", null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void revokeCertificateReturnsNotFound() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        when(certificateService.revokeCertificate("missing", null))
                .thenThrow(new IllegalArgumentException("missing"));

        ResponseEntity<Map<String, Object>> response = controller.revokeCertificate("missing", null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void revokeCertificateReturnsServerError() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        when(certificateService.revokeCertificate("cert-1", null))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.revokeCertificate("cert-1", null, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getStatisticsReturnsUnauthorized() {
        ResponseEntity<?> response = controller.getStatistics(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getStatisticsReturnsOkForAdmin() {
        Authentication authentication = authWithRole("admin-1", "ADMIN");
        when(certificateService.getStatistics()).thenReturn(Map.of("total", 1));

        ResponseEntity<?> response = controller.getStatistics(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void generateCertificateReturnsUnauthorizedWhenMissingAuth() {
        ResponseEntity<?> response = controller.generateCertificate("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void generateCertificateReturnsNotFoundWhenNotEnrolled() {
        Authentication authentication = authWithRole("user-1", "USER");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.generateCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generateCertificateReturnsBadRequestWhenIncomplete() {
        Authentication authentication = authWithRole("user-1", "USER");
        CourseProgress progress = sampleProgress();
        progress.setCompleted(false);
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));

        ResponseEntity<?> response = controller.generateCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void generateCertificateReturnsSuccessWhenCompleted() {
        Authentication authentication = authWithRole("user-1", "USER");
        CourseProgress progress = sampleProgress();
        progress.setCompleted(true);
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenReturn(Optional.of(progress));
        CertificateRecord certificateRecord = sampleRecord("CODE-7");
        when(certificateService.recordIssuance(progress, "https://certificates.agra.com/course/course-1/user/user-1/certificate.pdf", null))
                .thenReturn(certificateRecord);

        ResponseEntity<?> response = controller.generateCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("certificateCode", "CODE-7");
    }

    @Test
    void generateCertificateReturnsServerErrorOnException() {
        Authentication authentication = authWithRole("user-1", "USER");
        when(courseProgressService.getEnrollmentStatus("user-1", "course-1"))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.generateCertificate("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
