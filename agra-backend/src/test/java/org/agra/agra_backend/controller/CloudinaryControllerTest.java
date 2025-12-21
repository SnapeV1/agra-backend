package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryControllerTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private CloudinaryController controller;

    @Test
    void testConnectionReturnsOkWhenConnected() {
        when(cloudinaryService.testConnection()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.testConnection();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadImageRejectsNonImage() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("text/plain");

        ResponseEntity<Map<String, Object>> response = controller.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
