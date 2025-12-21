package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
    void testConnectionReturnsErrorWhenDisconnected() {
        when(cloudinaryService.testConnection()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.testConnection();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", "error");
        assertThat(response.getBody()).containsEntry("connected", false);
    }

    @Test
    void testConnectionReturnsErrorOnException() {
        when(cloudinaryService.testConnection()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.testConnection();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", "error");
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

    @Test
    void uploadImageReturnsOkOnSuccess() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinaryService.uploadImage(file))
                .thenReturn(Map.of("secure_url", "http://example.com", "public_id", "pid"));

        ResponseEntity<Map<String, Object>> response = controller.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        assertThat(response.getBody()).containsEntry("url", "http://example.com");
    }

    @Test
    void uploadImageReturnsErrorOnException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinaryService.uploadImage(file)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.uploadImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", "error");
    }

    @Test
    void uploadImageWithPresetRejectsEmptyFile() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.uploadImageWithPreset(file, "preset");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadImageWithPresetReturnsOkOnSuccess() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinaryService.uploadImage(file, "preset"))
                .thenReturn(new HashMap<>(Map.of("secure_url", "http://example.com", "public_id", "pid")));

        ResponseEntity<Map<String, Object>> response = controller.uploadImageWithPreset(file, "preset");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        assertThat(response.getBody()).containsEntry("preset_used", "preset");
    }

    @Test
    void uploadImageWithPresetReturnsErrorOnException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinaryService.uploadImage(file, "preset")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.uploadImageWithPreset(file, "preset");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", "error");
    }

    @Test
    void healthReturnsUp() {
        ResponseEntity<Map<String, String>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("service", "CloudinaryController");
        assertThat(response.getBody()).containsEntry("status", "UP");
    }
}
