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
import org.mockito.ArgumentCaptor;
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
        CertificateRecord record = new CertificateRecord();
        record.setId("cert-1");
        when(certificateRecordRepository.findById("cert-1")).thenReturn(Optional.of(record));
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
        assertThatThrownBy(() -> service.recordIssuance(null, "url", new Date()))
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

        CertificateRecord record = service.recordIssuance(progress, null, null);

        assertThat(record.getCertificateCode()).isNotBlank();
        assertThat(record.getCourseId()).isEqualTo("course-1");
        assertThat(record.getUserId()).isEqualTo("user-1");
        assertThat(record.getRecipientName()).isEqualTo("Student");
        assertThat(record.getCourseTitle()).isEqualTo("Course Title");
        assertThat(record.getCompletedAt()).isEqualTo(completionDate);
        assertThat(record.getCertificateUrl()).contains("course-1").contains("user-1");
    }

    @Test
    void verifyCertificateMarksRevoked() {
        CertificateRecord record = new CertificateRecord();
        record.setId("cert-1");
        record.setCertificateCode("ABC123");
        record.setCourseId("course-1");
        record.setUserId("user-1");
        record.setRevoked(true);
        record.setRevokedReason("Expired");
        when(certificateRecordRepository.findByCertificateCode("ABC123")).thenReturn(Optional.of(record));

        Optional<Map<String, Object>> result = service.verifyCertificate("ABC123");

        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("valid", false);
        assertThat(result.get()).containsEntry("message", "Expired");
    }
}
