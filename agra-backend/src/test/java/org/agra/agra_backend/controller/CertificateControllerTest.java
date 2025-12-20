package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.CertificateRecord;
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

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void validateCertificateReturnsNotFound() {
        when(certificateService.findByCode("code")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.validateCertificate("code");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("valid", false);
    }

    @Test
    void getCertificateForAuthenticatedUserReturnsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = controller.getCertificateForAuthenticatedUser("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCertificateForAuthenticatedUserReturnsNotFound() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);
        when(certificateService.findByCourseAndUser("course-1", "user-1")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getCertificateForAuthenticatedUser("course-1", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generateCertificateReturnsUnauthorizedWhenMissingAuth() {
        ResponseEntity<?> response = controller.generateCertificate("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
