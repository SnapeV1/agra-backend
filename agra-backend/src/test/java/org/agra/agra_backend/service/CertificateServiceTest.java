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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRecordRepository certificateRecordRepository;
    @Mock
    private CourseProgressRepository courseProgressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CertificateService service;

    @Test
    void updateMetadataUpdatesFields() {
        CertificateRecord certificateRecord = new CertificateRecord();
        certificateRecord.setId("cert-1");
        when(certificateRecordRepository.findById("cert-1")).thenReturn(Optional.of(certificateRecord));
        when(certificateRecordRepository.save(any(CertificateRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Date issuedAt = new Date();
        Date completedAt = new Date();

        CertificateRecord updated = service.updateMetadata("cert-1", "Instructor", "Org", issuedAt, completedAt, "Notes");

        assertThat(updated.getInstructorName()).isEqualTo("Instructor");
        assertThat(updated.getOrganizationName()).isEqualTo("Org");
        assertThat(updated.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(updated.getCompletedAt()).isEqualTo(completedAt);
        assertThat(updated.getNotes()).isEqualTo("Notes");
    }

    @Test
    void recordIssuanceThrowsWhenProgressMissing() {
        Date issuedAt = new Date();

        assertThatThrownBy(() -> service.recordIssuance(null, "url", issuedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course progress is required");
    }

    @Test
    void recordIssuanceGeneratesCodeAndSaves() {
        CourseProgress progress = new CourseProgress();
        progress.setId("progress-1");
        progress.setCourseId("course-1");
        progress.setUserId("user-1");
        progress.setEnrolledAt(new Date());
        Map<String, Date> completionDates = new HashMap<>();
        Date completionDate = new Date();
        completionDates.put("lesson-1", completionDate);
        progress.setLessonCompletionDates(completionDates);

        when(certificateRecordRepository.findByCertificateCode(any())).thenReturn(Optional.empty());
        when(certificateRecordRepository.findByCourseIdAndUserId("course-1", "user-1"))
                .thenReturn(Optional.empty());
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(certificateRecordRepository.save(any(CertificateRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = new User();
        user.setId("user-1");
        user.setName("Student");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        Course course = new Course();
        course.setId("course-1");
        course.setTitle(null);
        CourseTranslation translation = new CourseTranslation();
        translation.setTitle("Course Title");
        course.setTranslations(new HashMap<>(Map.of("en", translation)));
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

        CertificateRecord certificateRecord = service.recordIssuance(progress, null, null);

        assertThat(certificateRecord.getCertificateCode()).isNotBlank();
        assertThat(certificateRecord.getCourseId()).isEqualTo("course-1");
        assertThat(certificateRecord.getUserId()).isEqualTo("user-1");
        assertThat(certificateRecord.getRecipientName()).isEqualTo("Student");
        assertThat(certificateRecord.getCourseTitle()).isEqualTo("Course Title");
        assertThat(certificateRecord.getCompletedAt()).isEqualTo(completionDate);
        assertThat(certificateRecord.getCertificateUrl()).contains("course-1").contains("user-1");
    }

    @Test
    void verifyCertificateMarksRevoked() {
        CertificateRecord certificateRecord = new CertificateRecord();
        certificateRecord.setId("cert-1");
        certificateRecord.setCertificateCode("ABC123");
        certificateRecord.setCourseId("course-1");
        certificateRecord.setUserId("user-1");
        certificateRecord.setRevoked(true);
        certificateRecord.setRevokedReason("Expired");
        when(certificateRecordRepository.findByCertificateCode("ABC123")).thenReturn(Optional.of(certificateRecord));

        Optional<Map<String, Object>> result = service.verifyCertificate("ABC123");

        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("valid", false);
        assertThat(result.get()).containsEntry("message", "Expired");
    }
}
